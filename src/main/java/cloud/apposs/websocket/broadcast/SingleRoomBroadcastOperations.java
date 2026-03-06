package cloud.apposs.websocket.broadcast;

import cloud.apposs.websocket.WSSession;
import cloud.apposs.websocket.distributed.IDistributedService;
import cloud.apposs.websocket.distributed.pubsub.IPubSubService;
import cloud.apposs.websocket.distributed.pubsub.PubSubType;
import cloud.apposs.websocket.distributed.pubsub.message.BroadcastMessage;
import cloud.apposs.websocket.protocol.Packet;
import cloud.apposs.websocket.protocol.PacketType;

import java.util.Arrays;
import java.util.Collection;

/**
 * SocketIO 单个房间广播操作
 */
public class SingleRoomBroadcastOperations implements BroadcastOperations {
    private final String namespace;

    private final String room;

    private final Collection<WSSession> sessions;

    private final IDistributedService distributedService;

    public SingleRoomBroadcastOperations(String namespace, String room, Collection<WSSession> sessions, IDistributedService distributedService) {
        this.namespace = namespace;
        this.room = room;
        this.sessions = sessions;
        this.distributedService = distributedService;
    }

    @Override
    public boolean send(Packet packet) throws Exception {
        // 先从当前分布式节点会话管理中获取到该连接，获取到则说明会话是连接到了当前节点
        for (WSSession session : sessions) {
            session.send(packet);
        }
        // 再发布分布式事件，通知其他节点指定房间内所有连接会话发送消息
        IPubSubService pubsubService = distributedService.getPubSubService();
        pubsubService.publish(PubSubType.BROADCAST, new BroadcastMessage(namespace, room, packet));
        return true;
    }

    @Override
    public boolean sendEvent(short event, Object... data) throws Exception {
        Packet packet = new Packet(PacketType.EVENT);
        packet.setEvent(event);
        packet.setData(Arrays.asList(data));
        return send(packet);
    }

    @Override
    public void disconnect() throws Exception {
        for (WSSession session : sessions) {
            session.disconnect();
        }
    }
}
