package cloud.apposs.websocket;

import cloud.apposs.websocket.broadcast.BroadcastOperations;
import cloud.apposs.websocket.namespace.Namespace;
import cloud.apposs.websocket.protocol.HandshakeData;
import cloud.apposs.websocket.protocol.Packet;
import cloud.apposs.websocket.protocol.PacketEncoder;
import cloud.apposs.websocket.protocol.PacketType;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

public abstract class WSSession {
    // 会话ID，每个客户端连接都会有一个唯一的会话ID
    protected final UUID sessionId;

    // 会话路径/命名空间，即客户端连接的路径，如 /socket.io/
    protected final String path;

    protected final AtomicBoolean disconnected = new AtomicBoolean(false);

    protected final WSConfig configuration;

    // 当前会话所属的命名空间
    protected final Namespace namespace;

    // 会话集，用于管理所有会话
    protected final WSSessionBox sessionBox;

    // 会话握手数据
    protected final HandshakeData handshakeData;

    public WSSession(UUID sessionId, String path, WSConfig configuration, Namespace namespace,
                     WSSessionBox sessionBox, HandshakeData handshakeData) {
        this.sessionId = sessionId;
        this.path = path;
        this.configuration = configuration;
        this.namespace = namespace;
        this.sessionBox = sessionBox;
        this.handshakeData = handshakeData;
        namespace.addSession(this);
    }

    public UUID getSessionId() {
        return sessionId;
    }

    public String getPath() {
        return path;
    }
    /**
     * 判断会话是否已经连接
     */
    public boolean isConnected() {
        return !disconnected.get();
    }

    public Namespace getNamespace() {
        return namespace;
    }

    public HandshakeData getHandshakeData() {
        return handshakeData;
    }

    public InetSocketAddress getRemoteAddress() {
        return handshakeData.getRemoteAddress();
    }

    public InetSocketAddress getLocalAddress() {
        return handshakeData.getLocalAddress();
    }

    /**
     * 获取指定房间内的所有客户端连接数量
     *
     * @param  room 房间名
     * @return 房间内的所有客户端连接数量
     */
    public int getCurrentRoomSize(String room) {
        return namespace.getRoomClientsInCluster(room);
    }

    /**
     * 获取当前客户端加入的所有房间
     *
     * @return 房间名集合
     */
    public Set<String> getAllRooms() {
        return namespace.getRooms(this);
    }

    /**
     * 当前客户端加入指定房间
     *
     * @param room 房间名
     */
    public void joinRoom(String room) {
        namespace.joinRoom(room, getSessionId());
    }

    /**
     * 当前客户端加入指定房间
     *
     * @param rooms 房间名
     */
    public void joinRooms(Set<String> rooms) {
        namespace.joinRooms(rooms, getSessionId());
    }

    /**
     * 当前客户端离开指定房间
     *
     * @param room 房间名
     */
    public void leaveRoom(String room) {
        namespace.leaveRoom(room, getSessionId());
    }

    /**
     * 当前客户端离开指定房间
     *
     * @param rooms 房间名
     */
    public void leaveRooms(Set<String> rooms) {
        namespace.leaveRooms(rooms, getSessionId());
    }

    /**
     * 获取当前命名空间下所有默认房间内的客户端连接进行后续广播操作
     */
    public BroadcastOperations getBroadcastOperations() {
        return namespace.getBroadcastOperations();
    }

    /**
     * 根据指定房间名获取房间内的所有客户端连接
     *
     * @param  room 房间名
     * @return 房间内的所有客户端连接
     */
    public BroadcastOperations getRoomOperations(String room) {
        return namespace.getRoomOperations(room);
    }

    /**
     * 根据指定房间名获取房间内的所有客户端连接
     *
     * @param  rooms 房间名
     * @return 房间内的所有客户端连接
     */
    public BroadcastOperations getRoomOperations(String... rooms) {
        return namespace.getRoomOperations(rooms);
    }

    /**
     * 发送消息数据包
     *
     * @param event 事件名称
     * @param data  数据包，可由业务自定义JSON对象格式
     */
    public void sendEvent(short event, Object data) throws Exception {
        Packet packet = new Packet();
        packet.setType(PacketType.EVENT);
        packet.setEvent(event);
        packet.setData(data);
        send(packet);
    }

    /**
     * 发送自定义消息数据包，底层会自动将数据包转换为字节数组再调用相应的Channel发送
     *
     * @param  packet 消息数据包
     * @return 异步操作句柄
     */
    public void send(Packet packet) throws Exception {
        if (disconnected.get()) {
            throw new IOException("session is disconnected");
        }
        handlePacketSend(PacketEncoder.encode(packet, configuration.getJsonSupport()));
    }

    /**
     * 断开客户端连接
     */
    public void disconnect() throws Exception {
        try {
            // 发送数据包通知客户端断开连接，并在发送完成后关闭连接
            handleChannelDisconnect();
        } finally {
            // 触发资源释放
            onChannelDisconnect();
        }
    }

    public void onChannelDisconnect() {
        disconnected.set(true);
        sessionBox.removeSession(sessionId);
        namespace.onDisconnect(this);
    }

    public abstract void handleChannelDisconnect() throws Exception;

    public abstract void handlePacketSend(byte[] packet) throws Exception;
}
