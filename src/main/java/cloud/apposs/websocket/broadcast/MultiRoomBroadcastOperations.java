package cloud.apposs.websocket.broadcast;

import cloud.apposs.websocket.WSSession;
import cloud.apposs.websocket.protocol.Packet;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * SocketIO 多个房间广播操作
 */
public class MultiRoomBroadcastOperations implements BroadcastOperations {
    private final Collection<BroadcastOperations> broadcastOperations;

    public MultiRoomBroadcastOperations(Collection<BroadcastOperations> broadcastOperations) {
        this.broadcastOperations = broadcastOperations;
    }

    @Override
    public void send(Packet packet) throws Exception {
        if (broadcastOperations == null || broadcastOperations.size() == 0) {
            return;
        }
        for (BroadcastOperations b : broadcastOperations) {
            b.send(packet);
        }
    }

    @Override
    public void sendEvent(short event, Object... data) throws Exception {
        if (broadcastOperations == null || broadcastOperations.size() == 0) {
            return;
        }
        for (BroadcastOperations b : broadcastOperations) {
            b.sendEvent(event, data);
        }
    }

    @Override
    public Collection<WSSession> getClients() {
        Set<WSSession> clients = new HashSet<WSSession>();
        if (broadcastOperations == null || broadcastOperations.size() == 0) {
            return clients;
        }
        for (BroadcastOperations b : broadcastOperations) {
            clients.addAll(b.getClients());
        }
        return clients;
    }

    @Override
    public void disconnect() throws Exception {
        if (broadcastOperations == null || broadcastOperations.size() == 0) {
            return;
        }
        for (BroadcastOperations b : broadcastOperations) {
            b.disconnect();
        }
    }
}
