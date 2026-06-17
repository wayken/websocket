package cloud.apposs.websocket.client;

import cloud.apposs.websocket.protocol.Packet;

/**
 * WSClientListener的适配器，提供空实现方便子类按需覆写
 */
public class WSClientListenerAdapter implements WSClientListener {
    @Override
    public void onConnect(WSClient client) {
    }

    @Override
    public void onCommand(WSClient client, Packet packet) {
    }

    @Override
    public void onDisconnect(WSClient client) {
    }

    @Override
    public void onError(WSClient client, Throwable cause) {
    }
}
