package cloud.apposs.websocket.client;

import cloud.apposs.websocket.protocol.Packet;

/**
 * WebSocket客户端事件监听器
 */
public interface WSClientListener {
    /**
     * 连接建立成功
     */
    void onConnect(WSClient client);

    /**
     * 收到服务端指令消息
     */
    void onCommand(WSClient client, Packet packet);

    /**
     * 连接断开
     */
    void onDisconnect(WSClient client);

    /**
     * 发生异常
     */
    void onError(WSClient client, Throwable cause);
}
