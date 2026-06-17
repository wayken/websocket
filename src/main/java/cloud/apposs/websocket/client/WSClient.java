package cloud.apposs.websocket.client;

import cloud.apposs.logger.Logger;
import cloud.apposs.websocket.protocol.Packet;
import cloud.apposs.websocket.protocol.PacketEncoder;
import cloud.apposs.websocket.protocol.PacketType;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * WebSocket客户端抽象基类，与具体网络实现无关，子类只需实现连接/断开/发送字节的底层操作即可
 */
public abstract class WSClient {
    protected final WSClientConfig config;
    protected final WSClientListener listener;

    private final AtomicBoolean connected = new AtomicBoolean(false);
    private final AtomicInteger reconnectCount = new AtomicInteger(0);

    public WSClient(WSClientConfig config, WSClientListener listener) {
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
     * 连接到服务端
     */
    public void connect() throws Exception {
        handleConnect();
    }

    /**
     * 发送指令消息
     *
     * @param command   指令名称
     * @param parameters 指令参数列表，可以是任意类型，最终会被序列化为JSON字符串
     */
    public void sendCommand(String command, Object... parameters) throws Exception {
        Packet packet = new Packet(PacketType.COMMAND);
        packet.setCommand(command);
        packet.getParameter().setArguments(Arrays.asList(parameters));
        send(packet);
    }

    /**
     * 发送RPC响应包
     *
     * @param commandId  请求指令ID
     * @param parameters 响应参数列表
     */
    public void sendResponse(String commandId, Object... parameters) throws Exception {
        Packet packet = new Packet(PacketType.COMMAND);
        packet.getMetadata().setCommandId(commandId);
        packet.getParameter().setArguments(Arrays.asList(parameters));
        send(packet);
    }

    /**
     * 发送数据包，主包发送后逐个发送附件帧
     *
     * @param packet 数据包对象，包含类型、指令名称和参数等信息
     */
    public void send(Packet packet) throws Exception {
        byte[] buffer = PacketEncoder.encode(packet, config.getJsonSupport());
        handleSend(buffer);
        // 发送附件帧
        List<ByteBuffer> attachments = packet.getAttachments();
        if (attachments != null) {
            for (ByteBuffer attachment : attachments) {
                handleSendBinary(attachment.array());
            }
        }
    }

    /**
     * 断开连接
     */
    public void disconnect() {
        handleDisconnect();
        connected.set(false);
    }

    /**
     * 关闭客户端并释放资源
     */
    public void shutdown() {
        disconnect();
        handleShutdown();
    }

    /**
     * 底层建立WebSocket连接
     */
    protected abstract void handleConnect() throws Exception;

    /**
     * 底层发送字节数据
     *
     * @param data 需要发送的字节数据，通常是经过PacketEncoder编码后的数据包内容
     */
    protected abstract void handleSend(byte[] data) throws Exception;

    /**
     * 底层发送二进制附件帧
     *
     * @param data 附件的字节数据
     */
    protected void handleSendBinary(byte[] data) throws Exception {
        handleSend(data);
    }

    /**
     * 底层断开WebSocket连接
     */
    protected abstract void handleDisconnect();

    /**
     * 底层释放所有资源
     */
    protected abstract void handleShutdown();

    /**
     * 子类在连接建立后调用
     */
    protected void onConnected() {
        connected.set(true);
        reconnectCount.set(0);
        listener.onConnect(this);
    }

    /**
     * 子类在收到消息后调用
     *
     * @param packet 收到的数据包对象，包含类型、指令名称和参数等信息
     */
    protected void onMessage(Packet packet) {
        listener.onCommand(this, packet);
    }

    /**
     * 子类在连接断开后调用
     */
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
