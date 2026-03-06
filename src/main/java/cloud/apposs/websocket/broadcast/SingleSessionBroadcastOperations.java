package cloud.apposs.websocket.broadcast;

import cloud.apposs.logger.Logger;
import cloud.apposs.websocket.WSSession;
import cloud.apposs.websocket.distributed.IDistributedService;
import cloud.apposs.websocket.distributed.pubsub.IPubSubService;
import cloud.apposs.websocket.distributed.pubsub.PubSubMessage;
import cloud.apposs.websocket.distributed.pubsub.PubSubType;
import cloud.apposs.websocket.distributed.pubsub.message.DispatchMessage;
import cloud.apposs.websocket.protocol.Packet;
import cloud.apposs.websocket.protocol.PacketType;

import java.util.Arrays;
import java.util.Map;
import java.util.UUID;

public class SingleSessionBroadcastOperations implements BroadcastOperations {
    private final String namespace;

    private final UUID sessionId;

    private final Map<UUID, WSSession> sessions;

    private final IDistributedService distributedService;

    public SingleSessionBroadcastOperations(String namespace, UUID sessionId, Map<UUID, WSSession> sessions, IDistributedService distributedService) {
        this.namespace = namespace;
        this.sessionId = sessionId;
        this.sessions = sessions;
        this.distributedService = distributedService;
    }

    @Override
    public boolean send(Packet packet) throws Exception {
        // 先从当前分布式节点会话管理中获取到该连接，获取到则说明会话是连接到了当前节点
        if (sessions.containsKey(sessionId)) {
            return sessions.get(sessionId).send(packet);
        }
        // 如果客户端连接没有在当前分布式节点会话管理中，说明当前连接还没有完成连接建立，或者已经断开连接了，需要通过分布式服务来获取到该连接所在的分布式节点
        IPubSubService pubsubService = distributedService.getPubSubService();
        String sourceNodeId = pubsubService.getNodeId();
        String remoteNodeId = pubsubService.getClientNodeId(namespace, sessionId);
        // 如果客户端连接没有在当前分布式节点会话管理中，说明当前连接还没有完成连接建立，或者已经断开连接了
        if (remoteNodeId == null) {
            Logger.warn("Session %s is not connected to any remote node, current node is %s", sessionId, sourceNodeId);
            return false;
        }
        // 正常情况下，不可能出现客户端连接在当前分布式节点会话管理中没有找到，但是在其他分布式节点上找到
        if (sourceNodeId.equals(remoteNodeId)) {
            Logger.warn("Session %s is connected to remote node %s, but current node is also %s, this should not happen", sessionId, remoteNodeId, sourceNodeId);
            return false;
        }
        // 如果客户端连接不在当前分布式节点会话管理中，说明当前连接已经完成连接建立了，但是在其他分布式节点上了，需要通过分布式服务来发布/订阅
        if (Logger.isDebugEnabled()) {
            Logger.debug("Session %s is connected to remote node %s, current node is %s", sessionId, remoteNodeId, sourceNodeId);
        }
        PubSubMessage message = new DispatchMessage(namespace, sessionId, packet);
        distributedService.getPubSubService().publish(PubSubType.DISPATCH, message);
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
