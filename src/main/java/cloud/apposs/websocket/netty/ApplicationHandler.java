package cloud.apposs.websocket.netty;

import cloud.apposs.ioc.BeanFactory;
import cloud.apposs.rest.RestConfig;
import cloud.apposs.rest.Restful;
import cloud.apposs.util.StrUtil;
import cloud.apposs.websocket.*;
import cloud.apposs.websocket.annotation.ServerEndpoint;
import cloud.apposs.websocket.commandar.CommandarInvocation;
import cloud.apposs.websocket.commandar.CommandarRouter;
import cloud.apposs.websocket.distributed.DistributedServiceFactory;
import cloud.apposs.websocket.distributed.IDistributedService;
import cloud.apposs.websocket.distributed.pubsub.IPubSubService;
import cloud.apposs.websocket.interceptor.CommandarInterceptor;
import cloud.apposs.websocket.interceptor.CommandarInterceptorSupport;
import cloud.apposs.websocket.listener.ApplicationListener;
import cloud.apposs.websocket.listener.ApplicationListenerSupport;
import cloud.apposs.websocket.listener.CommandarListener;
import cloud.apposs.websocket.listener.CommandarListenerSupport;
import cloud.apposs.websocket.namespace.NamespacesHub;
import cloud.apposs.websocket.protocol.JsonSupport;
import cloud.apposs.websocket.protocol.JsonSupportWrapper;
import cloud.apposs.websocket.scheduler.CancelableScheduler;
import cloud.apposs.websocket.scheduler.HashedWheelTimeoutScheduler;
import cloud.apposs.websocket.util.Orders;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ApplicationHandler {
    private final WSConfig configuration;

    private BeanFactory beanFactory;

    // 指令映射
    private final CommandarRouter commandarRouter;

    private final CommandarInvocation commandarInvocation;

    private final CommandarInterceptorSupport commandarInterceptorSupport = new CommandarInterceptorSupport();

    private final CommandarListenerSupport commandarListenerSupport = new CommandarListenerSupport();

    private final WSSessionBox sessionBox = new WSSessionBox();

    private final NamespacesHub namespacesHub;

    private final CancelableScheduler scheduler = new HashedWheelTimeoutScheduler();

    private final IDistributedService distributedService;

    private final WebSocketManager manager;

    // 框架监听服务管理
    private final ApplicationListenerSupport applicationListenerSupport = new ApplicationListenerSupport();

    // Restful MVC框架，用于HTTP协议的请求处理
    private final Restful<WSHttpRequest, WSHttpResponse> restful;

    // Netty IO处理器初始化
    private final SocketIOChannelInitializer pipeline;

    public ApplicationHandler(WSConfig configuration) throws Exception {
        // 初始化配置项
        if (configuration.getJsonSupport() == null) {
            try {
                getClass().getClassLoader().loadClass("com.fasterxml.jackson.databind.ObjectMapper");
                try {
                    Class<?> jjs = getClass().getClassLoader().loadClass("cloud.apposs.websocket.protocol.JacksonJsonSupport");
                    JsonSupport support = (JsonSupport) jjs.getConstructor().newInstance();
                    configuration.setJsonSupport(support);
                } catch (Exception e) {
                    throw new IllegalArgumentException(e);
                }
            } catch (ClassNotFoundException e) {
                throw new IllegalArgumentException("Can't find jackson lib in classpath", e);
            }
        }
        configuration.setJsonSupport(new JsonSupportWrapper(configuration.getJsonSupport()));
        this.configuration = configuration;
        beanFactory = new BeanFactory();
        commandarRouter = new CommandarRouter(configuration);
        commandarInvocation = new CommandarInvocation(beanFactory);
        // 将Config配置注入IOC容器中，方便Endpoint直接通过@Autowired来获取Config配置
        beanFactory.addBean(configuration);
        // 初始化全局IOC容器上下文
        WSContextHolder contextHolder = new WSContextHolder(configuration, beanFactory);
        beanFactory.addBean(contextHolder);
        // 初始化IOC容器，从WebX框架配置扫描包路径中扫描所有Bean实例
        String basePackages = configuration.getBasePackage();
        if (StrUtil.isEmpty(basePackages)) {
            throw new IllegalStateException("base package not setting");
        }
        // 判断是否是以cloud.apposs.xxx, com.example.*作为多包扫描
        String[] basePackageSplit = basePackages.split(",");
        String[] basePackageList = new String[basePackageSplit.length];
        for (int i = 0; i < basePackageSplit.length; i++) {
            basePackageList[i] = basePackageSplit[i].trim();
        }
        // 扫描包将各个IOC组件添加进容器中
        beanFactory.load(basePackageList);
        // 扫描basePackage包下所有的ServerEndpoint注解类，并注册到命名空间中和Commandar处理器中
        List<Class<?>> endpointClassList = beanFactory.getClassAnnotationList(ServerEndpoint.class);
        // 初始化分布式服务
        distributedService = DistributedServiceFactory.newDistributedService(configuration.getDistributedType(), configuration);
        namespacesHub = new NamespacesHub(configuration, distributedService);
        for (Class<?> endpointClass : endpointClassList) {
            ServerEndpoint serverEndpoint = endpointClass.getAnnotation(ServerEndpoint.class);
            // 初始化命名空间
            String[] pathList = serverEndpoint.value();
            for (int i = 0; i < pathList.length; i++) {
                String path = pathList[i];
                if (!path.startsWith("/")) {
                    path = "/" + path;
                }
                if (!path.endsWith("/")) {
                    path = path + "/";
                }
                namespacesHub.create(path);
            }
            // 初始化Commandar处理器，获取并遍历该ServerEndpoint类中所有的方法，建立RouterPath -> Commandar映射匹配
            Method[] methods = endpointClass.getDeclaredMethods();
            for (Method method : methods) {
                commandarRouter.addCommandar(endpointClass, method);
            }
        }
        // 初始化容器监听服务
        List<CommandarListener> commandarListeners = beanFactory.getBeanHierarchyList(CommandarListener.class);
        for (CommandarListener listener : commandarListeners) {
            listener.initialize(configuration);
            commandarListenerSupport.addListener(listener);
        }
        List<ApplicationListener> appListenerList = beanFactory.getBeanHierarchyList(ApplicationListener.class);
        for (ApplicationListener listener : appListenerList) {
            listener.onInitialize(configuration);
            applicationListenerSupport.addListener(listener);
        }
        // 初始化拦截器
        List<CommandarInterceptor> interceptorList = beanFactory.getBeanHierarchyList(CommandarInterceptor.class);
        // 对拦截器进行排序后添加
        Orders.sortByOrderAnnotation(interceptorList);
        for (CommandarInterceptor interceptor : interceptorList) {
            commandarInterceptorSupport.addInterceptor(interceptor);
        }
        manager = new WebSocketManager(namespacesHub, scheduler, distributedService,
                commandarRouter, commandarInvocation, commandarInterceptorSupport, commandarListenerSupport);
        // 初始化Restful MVC框架，用于HTTP协议处理
        RestConfig restConfig = new RestConfig();
        restConfig.setBasePackage(basePackages);
        restful = new Restful<WSHttpRequest, WSHttpResponse>(restConfig, beanFactory);
        // 将WSConfig注入Restful的IOC容器，方便HTTP Action通过构造函数注入
        restful.getBeanFactory().addBean(configuration);
        restful.getBeanFactory().addBean(contextHolder);
        restful.getBeanFactory().addBean(namespacesHub);
        restful.addParameterResolver(new NettyParameterResolver());
        restful.addParameterResolver(new NettyVariableParameterResolver());
        restful.addParameterResolver(new NettyModelParameterResolver());
        restful.addViewResolver(new NettyViewResolver().build(restConfig));
        restful.initialize();
        pipeline = new SocketIOChannelInitializer(manager, sessionBox);
        pipeline.initialize(configuration, restful);
        applicationListenerSupport.onStartup(configuration);
    }

    public SocketIOChannelInitializer getPipeline() {
        return pipeline;
    }

    /**
     * 应用关闭时调用，进行资源清理等操作，包括
     * <pre>
     *      1. 注销所有会话的分布式订阅
     * </pre>
     */
    public void shutdown() {
        Map<UUID, WSSession> sessions = sessionBox.getSessionBox();
        IPubSubService pubsubService = distributedService.getPubSubService();
        for (Map.Entry<UUID, WSSession> socketIOSessionEntry : sessions.entrySet()) {
            UUID sessionId = socketIOSessionEntry.getKey();
            WSSession session = socketIOSessionEntry.getValue();
            pubsubService.unregisterSession(session.getNamespace().getName(), sessionId);
        }
        distributedService.shutdown();
        beanFactory.destroy();
        if (restful != null) {
            restful.destroy();
        }
        applicationListenerSupport.onShutdown();
    }
}
