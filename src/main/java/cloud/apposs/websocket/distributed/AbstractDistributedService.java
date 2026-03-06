package cloud.apposs.websocket.distributed;

import cloud.apposs.websocket.WSConfig;
import cloud.apposs.websocket.WSSession;
import cloud.apposs.websocket.distributed.pubsub.IPubSubService;
import cloud.apposs.websocket.distributed.pubsub.PubSubType;
import cloud.apposs.websocket.distributed.pubsub.message.*;
import cloud.apposs.websocket.namespace.Namespace;
import cloud.apposs.websocket.namespace.NamespacesHub;

import java.util.Set;
import java.util.UUID;

public abstract class AbstractDistributedService implements IDistributedService {
    protected final WSConfig configuration;

    public AbstractDistributedService(WSConfig configuration) {
        this.configuration = configuration;
    }

    @Override
    public void initialize(final WSConfig configuration, final NamespacesHub namespacesHub) {
        IPubSubService pubsubService = getPubSubService();

        // 接收到远端分发消息，转发到本地对应的session进行处理
        pubsubService.subscribe(PubSubType.DISPATCH, message -> {
            Namespace namespace = namespacesHub.get(message.getNamespace());
            if (namespace != null) {
                WSSession session = namespace.getSession(message.getSessionId());
                if (session != null) {
                    try {
                        session.send(message.getPacket());
                    } catch (Exception ignore) {
                    }
                }
            }
        }, DispatchMessage.class);

        // 接收到远端批量分发消息，转发到本地对应的session进行处理
        pubsubService.subscribe(PubSubType.BULK_DISPATCH, message -> {
            Namespace namespace = namespacesHub.get(message.getNamespace());
            if (namespace != null) {
                Set<UUID> sessionIds = message.getSessionIds();
                for (UUID sessionId : sessionIds) {
                    WSSession session = namespace.getSession(sessionId);
                    if (session != null) {
                        try {
                            session.send(message.getPacket());
                        } catch (Exception ignore) {
                        }
                    }
                }
            }
        }, BulkDispatchMessage.class);

        // 接收到远端指定房间广播消息，转发到本地对应的namespace进行处理
        pubsubService.subscribe(PubSubType.BROADCAST, message -> {
            String room = message.getRoom();
            try {
                namespacesHub.get(message.getNamespace()).broadcast(room, message.getPacket());
            } catch (Exception ignore) {
            }
        }, BroadcastMessage.class);

        // 接收到远端加入房间消息，转发到本地对应的namespace进行处理
        pubsubService.subscribe(PubSubType.JOIN, message -> {
            String name = message.getRoom();
            Namespace namespace = namespacesHub.get(message.getNamespace());
            if (namespace != null) {
                namespace.join(name, message.getSessionId());
            }
        }, JoinLeaveMessage.class);

        // 接收到远端批量加入房间消息，转发到本地对应的namespace进行处理
        pubsubService.subscribe(PubSubType.BULK_JOIN, message -> {
            Set<String> rooms = message.getRooms();
            for (String room : rooms) {
                Namespace namespace = namespacesHub.get(message.getNamespace());
                if (namespace != null) {
                    namespace.join(room, message.getSessionId());
                }
            }
        }, BulkJoinLeaveMessage.class);

        // 接收到远端离开房间消息，转发到本地对应的namespace进行处理
        pubsubService.subscribe(PubSubType.LEAVE, message -> {
            String name = message.getRoom();
            Namespace n = namespacesHub.get(message.getNamespace());
            if (n != null) {
                n.leave(name, message.getSessionId());
            }
        }, JoinLeaveMessage.class);

        // 接收到远端批量离开房间消息，转发到本地对应的namespace进行处理
        pubsubService.subscribe(PubSubType.BULK_LEAVE, message -> {
            Set<String> rooms = message.getRooms();
            for (String room : rooms) {
                Namespace namespace = namespacesHub.get(message.getNamespace());
                if (namespace != null) {
                    namespace.leave(room, message.getSessionId());
                }
            }
        }, BulkJoinLeaveMessage.class);
    }
}
