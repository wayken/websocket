package cloud.apposs.websocket;

import cloud.apposs.websocket.broadcast.BroadcastOperations;
import cloud.apposs.websocket.distributed.pubsub.IPubSubService;
import cloud.apposs.websocket.namespace.Namespace;
import cloud.apposs.websocket.netty.WebSocketContextHolder;
import cloud.apposs.websocket.protocol.HandshakeData;
import cloud.apposs.websocket.protocol.Packet;
import cloud.apposs.websocket.protocol.PacketEncoder;
import cloud.apposs.websocket.protocol.PacketType;
import cloud.apposs.websocket.scheduler.SchedulerKey;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
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

    private final WebSocketContextHolder contextHolder;

    // 当前会话请求存储的一些状态值
    private final Map<Object, Object> attributes = new ConcurrentHashMap<>(1);

    public WSSession(
            UUID sessionId,
            String path,
            WSConfig configuration,
            Namespace namespace,
            WSSessionBox sessionBox,
            HandshakeData handshakeData,
            WebSocketContextHolder contextHolder
    ) {
        this.sessionId = sessionId;
        this.path = path;
        this.configuration = configuration;
        this.namespace = namespace;
        this.sessionBox = sessionBox;
        this.handshakeData = handshakeData;
        this.contextHolder = contextHolder;
        namespace.addSession(this);
    }

    public UUID getSessionId() {
        return sessionId;
    }

    public String getPath() {
        return path;
    }

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

    public Object getAttribute(Object key) {
        return getAttribute(key, null);
    }

    /**
     * 获取指定会话请求存储的状态值
     *
     * @param  key        状态键
     * @param  defaultVal 默认值
     * @return 状态值
     */
    public Object getAttribute(Object key, Object defaultVal) {
        Object attr = attributes.get(key);
        if (attr == null && defaultVal != null) {
            attr = defaultVal;
            attributes.put(key, attr);
        }
        return attr;
    }

    /**
     * 设置指定会话请求存储的状态值
     *
     * @param  key   状态键
     * @param  value 状态值
     * @return 之前的状态值
     */
    public Object setAttribute(Object key, Object value) {
        return attributes.put(key, value);
    }

    /**
     * 判断指定会话请求存储的状态值是否存在
     *
     * @param  key 状态键
     * @return 状态值是否存在
     */
    public boolean hasAttribute(Object key) {
        return attributes.containsKey(key);
    }

    /**
     * 删除指定会话请求存储的状态值
     *
     * @param  key 状态键
     * @return 被移除的状态值
     */
    public Object removeAttribute(Object key) {
        return attributes.remove(key);
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
    public boolean send(Packet packet) throws Exception {
        if (disconnected.get()) {
            throw new IOException("session is disconnected");
        }
        return handlePacketSend(PacketEncoder.encode(packet, configuration.getJsonSupport()));
    }

    /**
     * 获取分布式环境下指定会话ID的客户端连接
     *
     * @param  sessionId 客户端连接ID
     * @return 客户端连接对象，如果当前实例没有该连接，则返回null
     */
    public BroadcastOperations getDistributedSessionOperations(UUID sessionId) {
        return namespace.getSessionOperations(sessionId);
    }

    /**
     * 获取分布式环境下指定会话ID集合的客户端连接
     *
     * @param  sessionIds 客户端连接ID集合
     * @return 客户端连接对象集合
     */
    public BroadcastOperations getDistributedSessionOperations(Collection<UUID> sessionIds) {
        return namespace.getMultiSessionOperations(sessionIds);
    }

    /**
     * 获取当前命名空间下所有默认房间内的客户端连接进行后续广播操作
     */
    public BroadcastOperations getDistributedRoomOperations() {
        return namespace.getBroadcastOperations();
    }

    /**
     * 根据指定房间名获取房间内的所有客户端连接
     *
     * @param  room 房间名
     * @return 房间内的所有客户端连接
     */
    public BroadcastOperations getDistributedRoomOperations(String room) {
        return namespace.getRoomOperations(room);
    }

    /**
     * 根据指定房间名获取房间内的所有客户端连接
     *
     * @param  rooms 房间名
     * @return 房间内的所有客户端连接
     */
    public BroadcastOperations getDistributedRoomOperations(Collection<String> rooms) {
        return namespace.getRoomOperations(rooms);
    }

    /**
     * 获取当前客户端加入的所有房间
     *
     * @return 房间名集合
     */
    public Set<String> getAllRooms() {
        return namespace.getSessionDistributedRooms(sessionId);
    }

    /**
     * 当前客户端加入指定房间
     *
     * @param room 房间名
     */
    public void joinRoom(String room) {
        namespace.joinRoom(room, sessionId);
    }

    /**
     * 当前客户端加入指定房间
     *
     * @param rooms 房间名
     */
    public void joinRooms(Set<String> rooms) {
        namespace.joinRooms(rooms, sessionId);
    }

    /**
     * 当前客户端离开指定房间
     *
     * @param room 房间名
     */
    public void leaveRoom(String room) {
        namespace.leaveRoom(room, sessionId);
    }

    /**
     * 当前客户端离开指定房间
     *
     * @param rooms 房间名
     */
    public void leaveRooms(Set<String> rooms) {
        namespace.leaveRooms(rooms, sessionId);
    }

    public void scheduleRenewal() {
        cancelRenewal();
        // 创建新的分布式服务注册续期检测任务
        SchedulerKey key = new SchedulerKey(SchedulerKey.Type.RENEWAL, sessionId);
        contextHolder.getScheduler().schedule(key, () -> {
            WSSession session = sessionBox.getSession(sessionId);
            if (session != null && session.isChannelOpen()) {
                IPubSubService pubSubService = namespace.getPubSubService();
                pubSubService.registerSession(namespace.getName(), sessionId);
                scheduleRenewal();
            }
        }, configuration.getRenewalInterval(), TimeUnit.MILLISECONDS);
    }

    public void cancelRenewal() {
        SchedulerKey key = new SchedulerKey(SchedulerKey.Type.RENEWAL, sessionId);
        contextHolder.getScheduler().cancel(key);
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
        cancelRenewal();
        disconnected.set(true);
        sessionBox.removeSession(sessionId);
        namespace.onDisconnect(this);
    }

    public abstract boolean isChannelOpen();

    public abstract void handleChannelDisconnect() throws Exception;

    public abstract boolean handlePacketSend(byte[] packet) throws Exception;
}
