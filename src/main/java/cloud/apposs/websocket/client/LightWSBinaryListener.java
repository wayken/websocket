package cloud.apposs.websocket.client;

public interface LightWSBinaryListener {
    /**
     * 连接建立成功
     *
     * @param client 当前客户端实例
     */
    void onConnect(LightWSClient client);

    /**
     * 收到服务端原始数据
     *
     * @param client 当前客户端实例
     * @param data 原始数据
     */
    void onBinaryReceived(LightWSClient client, byte[] data) throws Exception;

    /**
     * 连接断开
     *
     * @param client 当前客户端实例
     */
    void onDisconnect(LightWSClient client);

    /**
     * 发生异常
     *
     * @param client 当前客户端实例
     * @param cause 异常原因
     */
    void onError(LightWSClient client, Throwable cause);
}
