package cloud.apposs.websocket.client;

import cloud.apposs.websocket.client.netty.NettyLightWSClient;

/**
 * 轻量级WebSocket客户端启动入口，全局单例，提供创建、连接、关闭客户端的静态方法
 *
 * 使用示例：
 * <pre>
 * WSClientConfig config = new WSClientConfig();
 * config.setHost("127.0.0.1");
 * config.setPort(7010);
 * config.setPath("/socket.io");
 *
 * LightWSClient client = LightWebSocketClient.connect(config, new LightWSBinaryListenerAdapter() {
 *     &#64;Override
 *     public void onConnect(LightWSClient client) {
 *         System.out.println("Connected!");
 *     }
 *     &#64;Override
 *     public void onBinaryReceived(LightWSClient client, byte[] data) {
 *         System.out.println("Received: " + packet.getCommand());
 *     }
 * });
 *
 * client.send("Hello!");
 * </pre>
 */
public class LightWebSocketClient {
    /**
     * 创建并连接WebSocket客户端（使用默认Netty实现）
     *
     * @param config   客户端配置
     * @param listener 客户端事件监听器
     */
    public static LightWSClient connect(WSClientConfig config, LightWSBinaryListener listener) throws Exception {
        LightWSClient client = build(config, listener);
        client.connect();
        return client;
    }

    /**
     * 仅创建客户端实例（不自动连接），可手动调用connect()
     *
     * @param config   客户端配置
     * @param listener 客户端事件监听器
     */
    public static LightWSClient build(WSClientConfig config, LightWSBinaryListener listener) {
        return new NettyLightWSClient(config, listener);
    }

    /**
     * 关闭客户端
     *
     * @param client 客户端实例
     */
    public static void shutdown(LightWSClient client) {
        if (client != null) {
            client.shutdown();
        }
    }
}
