package cloud.apposs.websocket.client;

import cloud.apposs.logger.Logger;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 轻量级 WebSocket 客户端，不使用框架的 PacketEncoder/Decoder，直接收发原始二进制帧
 */
public abstract class LightWSClient {
    protected final WSClientConfig config;

    protected final LightWSBinaryListener listener;

    private final AtomicBoolean connected = new AtomicBoolean(false);

    private final AtomicInteger reconnectCount = new AtomicInteger(0);

    public LightWSClient(WSClientConfig config, LightWSBinaryListener listener) {
        this.config = config;
        this.listener = listener;
    }

    public WSClientConfig getConfig() {
        return config;
    }

    public boolean isConnected() {
        return connected.get();
    }

    /**
     * 连接到 WebSocket 服务端
     */
    public void connect() throws Exception {
        handleConnect();
    }

    public void send(byte[] data) throws Exception {
        handleSend(data);
    }

    public void send(String payload) throws Exception {
        send(payload.getBytes(StandardCharsets.UTF_8));
    }

    public void disconnect() {
        handleDisconnect();
        connected.set(false);
    }

    public void shutdown() {
        disconnect();
        handleShutdown();
    }

    // 子类在连接建立后调用
    protected void onConnected() {
        connected.set(true);
        reconnectCount.set(0);
        listener.onConnect(this);
    }

    /**
     * 子类在收到消息后调用
     *
     * @param data 收到的原始数据包
     */
    protected void onBinaryReceived(byte[] data) throws Exception {
        listener.onBinaryReceived(this, data);
    }

    // 子类在连接断开后调用
    protected void onDisconnected() {
        boolean wasConnected = connected.getAndSet(false);
        if (wasConnected) {
            listener.onDisconnect(this);
        }
        if (config.isReconnectOn()) {
            scheduleReconnect();
        }
    }

    /**
     * 子类在发生异常时调用
     */
    protected void onError(Throwable cause) {
        listener.onError(this, cause);
    }

    /**
     * 底层建立WebSocket连接
     */
    protected abstract void handleConnect() throws Exception;

    /**
     * 底层发送字节数据
     *
     * @param data 需要发送的字节数据
     */
    protected abstract void handleSend(byte[] data) throws Exception;

    /**
     * 底层断开WebSocket连接
     */
    protected abstract void handleDisconnect();

    /**
     * 底层释放所有资源
     */
    protected abstract void handleShutdown();

    private void scheduleReconnect() {
        int max = config.getMaxReconnectAttempts();
        if (max != -1 && reconnectCount.get() >= max) {
            Logger.info("WebSocket Client max reconnect attempts reached: %d", max);
            return;
        }
        reconnectCount.incrementAndGet();
        Logger.info("WebSocket Client reconnecting in %dms (attempt %d)", config.getReconnectInterval(), reconnectCount.get());
        handleScheduleReconnect(config.getReconnectInterval());
    }

    /**
     * 子类实现延迟重连调度
     */
    protected abstract void handleScheduleReconnect(int delayMs);
}
