package cloud.apposs.websocket.broadcast;

import cloud.apposs.websocket.WSSession;
import cloud.apposs.websocket.protocol.Packet;

import java.util.Collection;

/**
 * SocketIO 广播操作接口
 */
public interface BroadcastOperations {
    /**
     * 发送自定义消息包给所有连接的客户端
     *
     * @param packet 消息包
     */
    void send(Packet packet) throws Exception;

    /**
     * 发送消息包给所有连接的客户端
     *
     * @param event 事件名称
     * @param data  事件数据
     */
    void sendEvent(short event, Object ... data) throws Exception;

    /**
     * 获取所有要广播的客户端
     */
    Collection<WSSession> getClients();

    /**
     * 断开所有连接的客户端
     */
    void disconnect() throws Exception;
}
