package cloud.apposs.websocket.client;

import cloud.apposs.websocket.client.netty.NettyWSClient;

/**
 * WebSocket客户端启动入口，全局单例，提供创建、连接、关闭客户端的静态方法
 *
 * 使用示例：
 * <pre>
 * WSClientConfig config = new WSClientConfig();
 * config.setHost("127.0.0.1");
 * config.setPort(7010);
 * config.setPath("/socket.io");
 *
 * WSClient client = WebSocketClient.connect(config, new WSClientListenerAdapter() {
 *     &#64;Override
 *     public void onConnect(WSClient client) {
 *         System.out.println("Connected!");
 *     }
 *     &#64;Override
 *     public void onCommand(WSClient client, Packet packet) {
 *         System.out.println("Received: " + packet.getCommand());
 *     }
 * });
 *
 * client.sendCommand("chat", "Hello!");
 * </pre>
 */
public class WebSocketClient {

    /**
     * 创建并连接WebSocket客户端（使用默认Netty实现）
     */
    public static WSClient connect(WSClientConfig config, WSClientListener listener) throws Exception {
        WSClient client = build(config, listener);
        client.connect();
        return client;
    }

    /**
     * 仅创建客户端实例（不自动连接），可手动调用connect()
     */
    public static WSClient build(WSClientConfig config, WSClientListener listener) {
        return new NettyWSClient(config, listener);
    }

    /**
     * 关闭客户端
     */
    public static void shutdown(WSClient client) {
        if (client != null) {
            client.shutdown();
        }
    }
}
