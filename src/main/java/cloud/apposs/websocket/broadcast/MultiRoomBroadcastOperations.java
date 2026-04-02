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
    public boolean sendCommand(String command, Object... data) throws Exception {
        if (broadcastOperations == null || broadcastOperations.size() == 0) {
            return false;
        }
        for (BroadcastOperations b : broadcastOperations) {
            b.sendCommand(command, data);
        }
        return true;
    }

    @Override
    public void sendResponse(String id, Object... parameter) throws Exception {
        if (broadcastOperations == null || broadcastOperations.size() == 0) {
            return;
        }
        for (BroadcastOperations b : broadcastOperations) {
            b.sendResponse(id, parameter);
        }
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
