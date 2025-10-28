package cloud.apposs.websocket.protocol;

import java.io.OutputStream;

/**
 * WebSocket JSON 解析接口，用于解析JSON数据，可由业务自行扩展，默认使用Jackson解析
 */
public interface JsonSupport {
    /**
     * 添加事件参数映射，方便通过客户端请求的URL和发送的事件名直接映射到指定的Commander参数
     * @param namespace    命名空间
     * @param command      事件名称，用户自定义事件是以方法名+@+事件名拼接
     * @param commandClass 事件Commander参数类型
     */
    void addEventMapping(String namespace, String command, Class<?> commandClass);

    /**
     * 将字节流解析为对象
     *
     * @param  namespace 命名空间
     * @param  content   字节流
     * @param  event     事件名称
     * @return 解析后的对象
     */
    Object readValue(String namespace, byte[] content, short event) throws Exception;

    /**
     * 将对象解析为字节流
     *
     * @param out   字节流
     * @param value 编码对象
     */
    void writeValue(OutputStream out, Object value) throws Exception;
}
