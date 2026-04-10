package cloud.apposs.websocket;

import cloud.apposs.logger.Logger;
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
import cloud.apposs.websocket.namespace.Namespace;
import cloud.apposs.websocket.namespace.NamespacesHub;
import cloud.apposs.websocket.protocol.Packet;
import cloud.apposs.websocket.scheduler.CancelableScheduler;
import cloud.apposs.websocket.validator.Validator;

import java.util.List;

/**
 * WebSocket全局上下文管理，用于保存全局共享对象，如命名空间，拦截器等
 */
public final class WebSocketManager {
    private final NamespacesHub namespacesHub;

    private final CancelableScheduler scheduler;

    private final IDistributedService distributedService;

    private final CommandarRouter commandarRouter;

    private final CommandarInvocation commandarInvocation;

    private final CommandarInterceptorSupport commandarInterceptorSupport;

    public WebSocketManager(
            NamespacesHub namespacesHub,
            CancelableScheduler scheduler,
            IDistributedService distributedService,
            CommandarRouter commandarRouter,
            CommandarInvocation commandarInvocation,
            CommandarInterceptorSupport commandarInterceptorSupport
    ) {
        this.namespacesHub = namespacesHub;
        this.scheduler = scheduler;
        this.distributedService = distributedService;
        this.commandarRouter = commandarRouter;
        this.commandarInvocation = commandarInvocation;
        this.commandarInterceptorSupport = commandarInterceptorSupport;
    }

    public NamespacesHub getNamespacesHub() {
        return namespacesHub;
    }

    public CancelableScheduler getScheduler() {
        return scheduler;
    }

    public CommandarInterceptorSupport getCommandarInterceptorSupport() {
        return commandarInterceptorSupport;
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

    public void onCommand(WSSession session, Packet packet) throws Exception {
        List<Commandar> onCommandList = commandarRouter.getCommandar(session.getPath(), packet.getCommand());
        if (onCommandList == null) {
            return;
        }
        for (Commandar commandar : onCommandList) {
            // 进行消息事件拦截器拦截，如果返回false则不再进行后续的指令匹配处理
            if (!commandarInterceptorSupport.onCommand(commandar, session, packet.getParameter().getArguments())) {
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
                    Validator.validate(commandar, argument);
                }
                commandarInvocation.invoke(commandar, arguments);
            } catch (Throwable ex) {
                cause = ex;
                throw ex;
            } finally {
                commandarInterceptorSupport.afterCompletion(commandar, session, cause);
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
}
