package cloud.apposs.websocket.broadcast;

import cloud.apposs.websocket.WSSession;
import cloud.apposs.websocket.distributed.IDistributedService;
import cloud.apposs.websocket.distributed.pubsub.IPubSubService;
import cloud.apposs.websocket.distributed.pubsub.PubSubType;
import cloud.apposs.websocket.distributed.pubsub.message.BulkDispatchMessage;
import cloud.apposs.websocket.protocol.Packet;
import cloud.apposs.websocket.protocol.PacketType;

import java.util.*;

public class MultiSessionBroadcastOperations implements BroadcastOperations {
    private final String namespace;

    private final Set<UUID> sessionIds;

    private final Map<UUID, WSSession> sessions;

    private final IDistributedService distributedService;

    public MultiSessionBroadcastOperations(String namespace, Set<UUID> sessionIds, Map<UUID, WSSession> sessions, IDistributedService distributedService) {
        this.namespace = namespace;
        this.sessionIds = sessionIds;
        this.sessions = sessions;
        this.distributedService = distributedService;
    }

    @Override
    public boolean send(Packet packet) throws Exception {
        // 先从当前分布式节点会话管理中获取到该连接，获取到则说明会话是连接到了当前节点
        Set<UUID> remoteSessionIds = new HashSet<>();
        IPubSubService pubsubService = distributedService.getPubSubService();
        for (UUID sessionId : sessionIds) {
            WSSession session = sessions.get(sessionId);
            if (session == null && pubsubService.isClientRegistered(namespace, sessionId)) {
                remoteSessionIds.add(sessionId);
                continue;
            }
            if (session != null) {
                session.send(packet);
            }
        }
        // 如果有远程会话，则发布分布式事件，通知其他节点发送消息
        if (!remoteSessionIds.isEmpty()) {
            pubsubService.publish(PubSubType.BULK_DISPATCH, new BulkDispatchMessage(namespace, remoteSessionIds, packet));
        }
        return true;
    }

    @Override
    public boolean sendEvent(short event, Object... data) throws Exception {
        Packet packet = new Packet();
        packet.setType(PacketType.EVENT);
        packet.setEvent(event);
        packet.setData(Arrays.asList(data));
        return send(packet);
    }

    @Override
    public void disconnect() {
    }
}
