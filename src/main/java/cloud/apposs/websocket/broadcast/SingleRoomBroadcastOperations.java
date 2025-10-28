package cloud.apposs.websocket.broadcast;

import cloud.apposs.websocket.WSSession;
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

    private final Collection<WSSession> clients;

    public SingleRoomBroadcastOperations(String namespace, String room, Collection<WSSession> clients) {
        this.namespace = namespace;
        this.room = room;
        this.clients = clients;
    }

    @Override
    public Collection<WSSession> getClients() {
        return clients;
    }

    @Override
    public void send(Packet packet) throws Exception {
        for (WSSession client : clients) {
            client.send(packet);
        }
    }

    @Override
    public void sendEvent(short event, Object... data) throws Exception {
        Packet packet = new Packet(PacketType.EVENT);
        packet.setEvent(event);
        packet.setData(Arrays.asList(data));
        send(packet);
    }

    @Override
    public void disconnect() throws Exception {
        for (WSSession client : clients) {
            client.disconnect();
        }
    }
}
