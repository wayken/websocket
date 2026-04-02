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
     * @param command 事件名称
     * @param data    事件数据
     */
    boolean sendCommand(String command, Object ... data) throws Exception;

    /**
     * 发送消息响应包，主要应用于 WEBSOCKET RPC 请求-响应通讯场景
     *
     * @param id        指令ID
     * @param parameter 数据包，可由业务自定义JSON对象格式
     */
    void sendResponse(String id, Object... parameter) throws Exception;

    /**
     * 断开所有连接的客户端
     */
    void disconnect() throws Exception;
}
