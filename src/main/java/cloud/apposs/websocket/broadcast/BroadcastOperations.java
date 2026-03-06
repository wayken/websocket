package cloud.apposs.websocket.broadcast;

import cloud.apposs.websocket.protocol.Packet;

/**
 * SocketIO 广播操作接口
 */
public interface BroadcastOperations {
    /**
     * 发送自定义消息包给所有连接的客户端
     *
     * @param packet 消息包
     */
    boolean send(Packet packet) throws Exception;

    /**
     * 发送消息包给所有连接的客户端
     *
     * @param event 事件名称
     * @param data  事件数据
     */
    boolean sendEvent(short event, Object ... data) throws Exception;

    /**
     * 断开所有连接的客户端
     */
    void disconnect() throws Exception;
}
