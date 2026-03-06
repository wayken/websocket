package cloud.apposs.websocket.broadcast;

import cloud.apposs.websocket.protocol.Packet;

import java.util.Collection;

/**
 * SocketIO 多个房间广播操作
 */
public class MultiRoomBroadcastOperations implements BroadcastOperations {
    private final Collection<BroadcastOperations> broadcastOperations;

    public MultiRoomBroadcastOperations(Collection<BroadcastOperations> broadcastOperations) {
        this.broadcastOperations = broadcastOperations;
    }

    @Override
    public boolean send(Packet packet) throws Exception {
        if (broadcastOperations == null || broadcastOperations.size() == 0) {
            return false;
        }
        for (BroadcastOperations b : broadcastOperations) {
            b.send(packet);
        }
        return true;
    }

    @Override
    public boolean sendEvent(short event, Object... data) throws Exception {
        if (broadcastOperations == null || broadcastOperations.size() == 0) {
            return false;
        }
        for (BroadcastOperations b : broadcastOperations) {
            b.sendEvent(event, data);
        }
        return true;
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
