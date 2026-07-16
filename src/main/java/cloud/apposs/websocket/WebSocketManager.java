package cloud.apposs.websocket;

import cloud.apposs.logger.Logger;
import cloud.apposs.util.Errno;
import cloud.apposs.websocket.annotation.OnConnect;
import cloud.apposs.websocket.annotation.OnDisconnect;
import cloud.apposs.websocket.annotation.OnError;
import cloud.apposs.websocket.commandar.Commandar;
import cloud.apposs.websocket.commandar.CommandarInvocation;
import cloud.apposs.websocket.commandar.CommandarRouter;
import cloud.apposs.websocket.commandar.ParameterResolver;
import cloud.apposs.websocket.distributed.IDistributedService;
import cloud.apposs.websocket.distributed.pubsub.IPubSubService;
import cloud.apposs.websocket.interceptor.CommandarInterceptorSupport;
import cloud.apposs.websocket.listener.CommandarListenerSupport;
import cloud.apposs.websocket.namespace.Namespace;
import cloud.apposs.websocket.namespace.NamespacesHub;
import cloud.apposs.websocket.protocol.Packet;
import cloud.apposs.websocket.resolver.exception.CommandExceptionResolver;
import cloud.apposs.websocket.scheduler.CancelableScheduler;
import cloud.apposs.rest.validator.IChecker;
import cloud.apposs.rest.validator.Validator;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

/**
 * WebSocket全局上下文管理，用于保存全局共享对象，如命名空间，拦截器等
 */
public final class WebSocketManager {
    private final WSConfig configuraion;

    private final NamespacesHub namespacesHub;

    private final WSSessionBox sessionBox;

    private final CancelableScheduler scheduler;

    private final IDistributedService distributedService;

    private final CommandarRouter commandarRouter;

    private final CommandarInvocation commandarInvocation;

    private final CommandarInterceptorSupport commandarInterceptorSupport;

    private final CommandarListenerSupport commandarListenerSupport;

    private final CommandExceptionResolver commandExceptionResolver;

    public WebSocketManager(
            WSConfig configuraion,
            NamespacesHub namespacesHub,
            WSSessionBox sessionBox,
            CancelableScheduler scheduler,
            IDistributedService distributedService,
            CommandarRouter commandarRouter,
            CommandarInvocation commandarInvocation,
            CommandarInterceptorSupport commandarInterceptorSupport,
            CommandarListenerSupport commandarListenerSupport,
            CommandExceptionResolver commandExceptionResolver
    ) {
        this.configuraion = configuraion;
        this.namespacesHub = namespacesHub;
        this.sessionBox = sessionBox;
        this.scheduler = scheduler;
        this.distributedService = distributedService;
        this.commandarRouter = commandarRouter;
        this.commandarInvocation = commandarInvocation;
        this.commandarInterceptorSupport = commandarInterceptorSupport;
        this.commandarListenerSupport = commandarListenerSupport;
        this.commandExceptionResolver = commandExceptionResolver;
    }

    public WSConfig getConfiguraion() {
        return configuraion;
    }

    public NamespacesHub getNamespacesHub() {
        return namespacesHub;
    }

    public WSSessionBox getSessionBox() {
        return sessionBox;
    }

    public CancelableScheduler getScheduler() {
        return scheduler;
    }

    public CommandarInterceptorSupport getCommandarInterceptorSupport() {
        return commandarInterceptorSupport;
    }

    public CommandarListenerSupport getCommandarListenerSupport() {
        return commandarListenerSupport;
    }

    public CommandarRouter getCommandarRouter() {
        return commandarRouter;
    }

    public CommandarInvocation getCommandarInvocation() {
        return commandarInvocation;
    }

    public void onConnect(WSSession session) throws Exception {
        // 注册当前客户端信息到分布式注册中心
        IPubSubService pubsubService = distributedService.getPubSubService();
        Namespace namespace = namespacesHub.get(session.getPath());
        pubsubService.registerSession(namespace.getName(), session.getSessionId());
        // 获取注解接口的 OnConnect 方法并执行连接成功回调
        List<Commandar> onConnectCommandList = commandarRouter.getCommandar(session.getPath(), OnConnect.class.getSimpleName());
        if (onConnectCommandList != null) {
            for (Commandar commandar : onConnectCommandList) {
                commandarInvocation.invoke(commandar, session);
            }
        }
    }

    public void onCommand(WSSession session, Packet packet) throws Throwable {
        List<Commandar> onCommandList = commandarRouter.getCommandar(session.getPath(), packet.getCommand());
        if (onCommandList == null) {
            return;
        }
        for (Commandar commandar : onCommandList) {
            List<Object> parameterArgument = packet.getParameter().getArguments();
            commandarListenerSupport.commandarStart(commandar, session, parameterArgument);
            // 进行消息事件拦截器拦截，如果返回false则不再进行后续的指令匹配处理
            if (!commandarInterceptorSupport.onCommand(commandar, session, parameterArgument)) {
                return;
            }
            Throwable cause = null;
            try {
                // 解析并校验参数
                Object[] arguments = ParameterResolver.resolveParameterArguments(commandar, session, packet);
                for (int i = 0; i < arguments.length; i++) {
                    Object argument = arguments[i];
                    if (argument == null || ParameterResolver.isSystemParameter(argument.getClass())) {
                        continue;
                    }
                    handleArgumentValidate(argument);
                }
                commandarInvocation.invoke(commandar, arguments);
            } catch (Throwable ex) {
                cause = ex;
                // 如果请求携带了commandId（即RPC请求），则将错误信息通过错误响应返回给客户端
                String commandId = packet.getMetadata().getCommandId();
                if (commandId != null && !commandId.isEmpty()) {
                    try {
                        handleCommandError(session, commandId, ex);
                    } catch (Throwable e) {
                        throw e;
                    }
                } else {
                    throw ex;
                }
            } finally {
                commandarInterceptorSupport.afterCompletion(commandar, session, cause);
                commandarListenerSupport.commandarCompletion(commandar, session, cause);
            }
        }
    }

    public void onDisconnect(WSSession session) throws Exception {
        // 从分布式注册中心注销客户端
        IPubSubService pubsubService = distributedService.getPubSubService();
        pubsubService.unregisterSession(session.getNamespace().getName(), session.getSessionId());
        // 获取注解接口的 OnDisconnect 方法并执行断开连接回调
        List<Commandar> onCommandList = commandarRouter.getCommandar(session.getPath(), OnDisconnect.class.getSimpleName());
        if (onCommandList != null) {
            for (Commandar commandar : onCommandList) {
                commandarInvocation.invoke(commandar, session);
            }
        }
    }

    public boolean onError(String path, Throwable cause) {
        // 获取注解接口的 OnError 方法并执行方法回调
        List<Commandar> onCommandList = commandarRouter.getCommandar(path, OnError.class.getSimpleName());
        if (onCommandList != null) {
            for (Commandar commandar : onCommandList) {
                try {
                    commandarInvocation.invoke(commandar, cause);
                } catch (Throwable ex) {
                    Logger.warn(ex, "Error during cause processing by commandar %s", commandar);
                }
            }
        }
        return onCommandList != null && !onCommandList.isEmpty();
    }

    private void handleArgumentValidate(Object model) {
        if (model == null) {
            return;
        }
        Class<?> clazz = model.getClass();
        while (clazz != null && clazz != Object.class) {
            for (java.lang.reflect.Field field : clazz.getDeclaredFields()) {
                Annotation[] annotations = field.getDeclaredAnnotations();
                if (annotations == null || annotations.length == 0) {
                    continue;
                }
                for (Annotation annotation : annotations) {
                    IChecker checker = Validator.getChecker(annotation.annotationType());
                    if (checker == null) {
                        continue;
                    }
                    boolean accessible = field.isAccessible();
                    try {
                        field.setAccessible(true);
                        Object value = field.get(model);
                        Object result = checker.check(field, annotation, value);
                        field.set(model, result);
                    } catch (IllegalArgumentException e) {
                        throw e;
                    } catch (Exception e) {
                        throw new IllegalArgumentException("field " + field.getName() + " validation failed: " + e.getMessage(), e);
                    } finally {
                        field.setAccessible(accessible);
                    }
                }
            }
            clazz = clazz.getSuperclass();
        }
    }

    // 处理指令执行异常，将错误信息通过带status的错误响应包返回给客户端
    private void handleCommandError(WSSession session, String commandId, Throwable cause) throws Throwable {
        if (cause instanceof InvocationTargetException) {
            cause = ((InvocationTargetException) cause).getTargetException();
        }
        if (commandExceptionResolver == null) {
            throw cause;
        }
        Errno result = commandExceptionResolver.resolveCommandException(commandId, cause);
        session.sendResponse(commandId, result.value(), result.description());
    }
}
