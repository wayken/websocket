package cloud.apposs.websocket.namespace;

import cloud.apposs.logger.Logger;
import cloud.apposs.websocket.WSConfig;
import cloud.apposs.websocket.WSSession;
import cloud.apposs.websocket.broadcast.*;
import cloud.apposs.websocket.distributed.IDistributedService;
import cloud.apposs.websocket.distributed.pubsub.IPubSubService;
import cloud.apposs.websocket.distributed.repository.IRepositoryService;
import cloud.apposs.websocket.protocol.Packet;
import io.netty.util.internal.PlatformDependent;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * SocketIO命名空间服务，
 * 建立WebSocket连接的时候，使用路径名来指定命名空间，在没有指定命名空间下，默认会使用 / 作为命名空间
 */
public final class Namespace {
    private final String name;

    private final WSConfig configuration;

    private final IDistributedService distributedService;

    // 该命名空间下的所有客户端连接
    private final Map<UUID, WSSession> sessionList = new ConcurrentHashMap<>();

    // 当前房间加入的所有客户端，方便通过房间名查找客户端连接
    private final ConcurrentMap<String, Set<UUID>> roomClients = PlatformDependent.newConcurrentHashMap();
    // 当前客户端加入的所有房间，方便通过客户端连接查找房间集合
    private final ConcurrentMap<UUID, Set<String>> clientRooms = PlatformDependent.newConcurrentHashMap();

    public Namespace(String name, WSConfig configuration, IDistributedService distributedService) {
        this.name = name;
        this.configuration = configuration;
        this.distributedService = distributedService;
    }

    public String getName() {
        return name;
    }

    public WSSession getSession(String sessionId) {
        return getSession(UUID.fromString(sessionId));
    }

    /**
     * 获取当前实例下指定会话ID的客户端连接
     *
     * @param  uuid 会话ID
     * @return 客户端连接，如果不存在则返回null
     */
    public WSSession getSession(UUID uuid) {
        return sessionList.get(uuid);
    }

    public void addSession(WSSession session) {
        sessionList.put(session.getSessionId(), session);
    }

    /**
     * 获取当前实例下的所有客户端会话连接
     *
     * @return 客户端会话连接集合
     */
    public Collection<WSSession> getSessions() {
        return Collections.unmodifiableCollection(sessionList.values());
    }

    /**
     * 获取分布式环境下当前命名空间下所有客户端连接的会话ID和所在实例ID映射关系
     *
     * @return 会话ID和所在实例ID映射关系
     */
    public Map<UUID, String> getDistributedSessions() {
        return distributedService.getPubSubService().getAllSessions(name);
    }

    /**
     * 获取指定房间内的所有客户端连接
     *
     * @param  room 房间名
     * @return 房间内的所有客户端连接
     */
    public Collection<WSSession> getRoomSessions(String room) {
        Set<UUID> sessionIds = roomClients.get(room);

        if (sessionIds == null) {
            return Collections.emptyList();
        }

        List<WSSession> result = new ArrayList<WSSession>(sessionIds.size());
        for (UUID sessionId : sessionIds) {
            WSSession session = sessionList.get(sessionId);
            if(session != null) {
                result.add(session);
            }
        }
        return result;
    }

    /**
     * 获取分布式环境下当前命名空间下所有客户端连接加入的所有房间
     *
     * @return 房间名集合
     */
    public Set<String> getAllDistributedRooms() {
        return roomClients.keySet();
    }

    /**
     * 获取指定客户端连接加入的所有房间
     *
     * @param sessionId 客户端连接ID
     * @return 房间名集合
     */
    public Set<String> getSessionDistributedRooms(UUID sessionId) {
        Set<String> rooms = clientRooms.get(sessionId);
        if (rooms == null) {
            return Collections.emptySet();
        }
        return Collections.unmodifiableSet(rooms);
    }

    /**
     * 获取分布式服务中的发布订阅服务，主要用于分布式环境下的消息广播和事件通知
     *
     * @return 发布订阅服务实例
     */
    public IPubSubService getPubSubService() {
        return distributedService.getPubSubService();
    }

    /**
     * 获取分布式服务中的数据存储服务，主要用于分布式环境下的自定义数据存储和查询
     *
     * @return 数据存储服务实例
     */
    public IRepositoryService getRepositoryService() {
        return distributedService.getRepositoryService();
    }

    /**
     * 获取分布式环境下指定会话ID的客户端连接
     *
     * @param  sessionId 客户端连接ID
     * @return 客户端连接对象，如果当前实例没有该连接，则返回null
     */
    public BroadcastOperations getSessionOperations(UUID sessionId) {
        if (!distributedService.getPubSubService().isClientRegistered(name, sessionId)) {
            return null;
        }
        return new SingleSessionBroadcastOperations(name, sessionId, sessionList, distributedService);
    }

    /**
     * 获取分布式环境下指定会话ID集合的客户端连接
     *
     * @param  sessionIds 客户端连接ID集合
     * @return 客户端连接对象集合
     */
    public BroadcastOperations getMultiSessionOperations(Collection<UUID> sessionIds) {
        Set<UUID> operationsList = new HashSet<>(sessionIds.size());
        IPubSubService pubsubService = distributedService.getPubSubService();
        for (UUID sessionId : sessionIds) {
            if (!pubsubService.isClientRegistered(name, sessionId)) {
                continue;
            }
            operationsList.add(sessionId);
        }
        return new MultiSessionBroadcastOperations(name, operationsList, sessionList, distributedService);
    }

    /**
     * 获取当前命名空间下所有默认房间内的客户端连接
     */
    public BroadcastOperations getBroadcastOperations() {
        return new SingleRoomBroadcastOperations(getName(), getName(), sessionList.values(), distributedService);
    }

    /**
     * 根据指定房间名获取房间内的所有客户端连接
     *
     * @param  room 房间名
     * @return 房间内的所有客户端连接
     */
    public BroadcastOperations getRoomOperations(String room) {
        return new SingleRoomBroadcastOperations(getName(), room, getRoomSessions(room), distributedService);
    }

    /**
     * 根据指定房间名获取房间内的所有客户端连接
     *
     * @param  rooms 房间名
     * @return 房间内的所有客户端连接
     */
    public BroadcastOperations getRoomOperations(Collection<String> rooms) {
        List<BroadcastOperations> roomList = new ArrayList<>();
        for (String room : rooms) {
            roomList.add(new SingleRoomBroadcastOperations(getName(), room, getRoomSessions(room), distributedService));
        }
        return new MultiRoomBroadcastOperations(roomList);
    }

    /**
     * 广播消息到指定房间的所有客户端连接
     *
     * @param room 房间名
     * @param packet 消息数据包
     */
    public void broadcast(String room, Packet packet) throws Exception {
        Iterable<WSSession> sessions = getRoomSessions(room);
        for (WSSession session : sessions) {
            session.send(packet);
        }
    }

    /**
     * 加入指定房间
     *
     * @param room 房间名
     * @param sessionId 客户端连接ID
     */
    public void joinRoom(String room, UUID sessionId) {
        join(room, sessionId);
    }

    /**
     * 加入指定房间
     *
     * @param rooms 房间名
     * @param sessionId 客户端连接ID
     */
    public void joinRooms(Set<String> rooms, final UUID sessionId) {
        for (String room : rooms) {
            join(room, sessionId);
        }
    }

    /**
     * 加入指定房间
     *
     * @param room 房间名
     * @param sessionId 客户端连接ID
     */
    public void join(String room, UUID sessionId) {
        handleJoinRoom(roomClients, room, sessionId);
        handleJoinRoom(clientRooms, sessionId, room);
    }

    /**
     * 离开指定房间
     *
     * @param room 房间名
     * @param sessionId 客户端连接ID
     */
    public void leaveRoom(String room, UUID sessionId) {
        leave(room, sessionId);
    }

    /**
     * 离开指定房间
     *
     * @param rooms 房间名
     * @param sessionId 客户端连接ID
     */
    public void leaveRooms(Set<String> rooms, final UUID sessionId) {
        for (String room : rooms) {
            leave(room, sessionId);
        }
    }

    /**
     * 离开指定房间
     *
     * @param room 房间名
     * @param sessionId 客户端连接ID
     */
    public void leave(String room, UUID sessionId) {
        handleLeaveRoom(roomClients, room, sessionId);
        handleLeaveRoom(clientRooms, sessionId, room);
    }

    private <K, V> void handleJoinRoom(ConcurrentMap<K, Set<V>> map, K key, V value) {
        Set<V> clients = map.get(key);
        if (clients == null) {
            clients = Collections.newSetFromMap(PlatformDependent.<V, Boolean>newConcurrentHashMap());
            Set<V> oldClients = map.putIfAbsent(key, clients);
            if (oldClients != null) {
                clients = oldClients;
            }
        }
        clients.add(value);
        // object may be changed due to other concurrent call
        if (clients != map.get(key)) {
            // re-join if queue has been replaced
            handleJoinRoom(map, key, value);
        }
    }

    private <K, V> void handleLeaveRoom(ConcurrentMap<K, Set<V>> map, K room, V sessionId) {
        Set<V> clients = map.get(room);
        if (clients == null) {
            return;
        }
        clients.remove(sessionId);

        if (clients.isEmpty()) {
            map.remove(room, Collections.emptySet());
        }
    }

    public void onDisconnect(WSSession session) {
        sessionList.remove(session.getSessionId());
        Set<String> joinedRooms = session.getAllRooms();
        for (String joinedRoom : joinedRooms) {
            handleLeaveRoom(roomClients, joinedRoom, session.getSessionId());
        }
        clientRooms.remove(session.getSessionId());
        Logger.debug("Client %s for namespace %s has been disconnected", session.getSessionId(), name);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        Namespace other = (Namespace) obj;
        if (name == null) {
            if (other.name != null) {
                return false;
            }
        } else if (!name.equals(other.name)) {
            return false;
        }
        return true;
    }
}
