package cloud.apposs.websocket.netty;

import cloud.apposs.logger.Logger;
import cloud.apposs.websocket.WSSession;
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

import java.util.List;

/**
 * SocketIO全局上下文，用于保存全局共享对象，如命名空间，拦截器等
 */
public final class WebSocketContextHolder {
    private final NamespacesHub namespacesHub;

    private final CancelableScheduler scheduler;

    private final IDistributedService distributedService;

    private final CommandarRouter commandarRouter;

    private final CommandarInvocation commandarInvocation;

    private final CommandarInterceptorSupport commandarInterceptorSupport;

    public WebSocketContextHolder(
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

    public void onEvent(WSSession session, Packet packet) throws Exception {
        List<Commandar> onEventCommandList = commandarRouter.getCommandar(session.getPath(), packet.getCommand());
        if (onEventCommandList == null) {
            return;
        }
        for (Commandar commandar : onEventCommandList) {
            // 进行消息事件拦截器拦截，如果返回false则不再进行后续的指令匹配处理
            if (!commandarInterceptorSupport.onEvent(commandar, session, packet.getData())) {
                return;
            }
            Throwable cause = null;
            try {
                Object[] args = ParameterResolver.resolveParameterArguments(commandar, session, packet.getData());
                commandarInvocation.invoke(commandar, args);
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
        List<Commandar> onDisconnectCommandList = commandarRouter.getCommandar(session.getPath(), OnDisconnect.class.getSimpleName());
        if (onDisconnectCommandList != null) {
            for (Commandar commandar : onDisconnectCommandList) {
                commandarInvocation.invoke(commandar, session);
            }
        }
    }

    public boolean onError(String path, Throwable cause) {
        // 获取注解接口的 OnError 方法并执行方法回调
        List<Commandar> onErrorCommandList = commandarRouter.getCommandar(path, OnError.class.getSimpleName());
        if (onErrorCommandList != null) {
            for (Commandar commandar : onErrorCommandList) {
                try {
                    commandarInvocation.invoke(commandar, cause);
                } catch (Throwable ex) {
                    Logger.warn(ex, "Error during cause processing by commandar %s", commandar);
                }
            }
        }
        return onErrorCommandList != null && !onErrorCommandList.isEmpty();
    }
}
