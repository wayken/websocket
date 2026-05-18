package cloud.apposs.websocket.listener;

import cloud.apposs.websocket.WSConfig;
import cloud.apposs.websocket.WSSession;
import cloud.apposs.websocket.commandar.Commandar;

import java.util.List;

/**
 * {@link cloud.apposs.websocket.commandar.Commandar}监听器，一般全局单例
 */
public interface CommandarListener {
    /**
     * 服务启动时的拦截器初始化，只调用一次
     */
    void initialize(WSConfig configuration);

    /**
     * 该方法在建立WebSocket之后客户端开始发送消息事件时调用的监听
     *
     * @param commandar 指令体
     * @param session   会话信息
     * @param argument  指令参数
     */
    void commandarStart(Commandar commandar, WSSession session, List<Object> argument);

    /**
     * 整个请求处理完毕时的监听，无论请求逻辑处理有没有成功，
     * 一般用于性能监控中在此记录结束时间并输出消耗时间
     *
     * @param commandar 指令体
     * @param session   会话信息
     * @param cause     如果业务调用产生了异常，则该值不为空
     */
    void commandarCompletion(Commandar commandar, WSSession session, Throwable cause);
}
