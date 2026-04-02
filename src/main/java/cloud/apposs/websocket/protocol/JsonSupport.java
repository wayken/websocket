package cloud.apposs.websocket.protocol;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

/**
 * WebSocket JSON 解析接口，用于解析JSON数据，可由业务自行扩展，默认使用Jackson解析
 */
public interface JsonSupport {
    /**
     * 添加事件参数映射，方便通过客户端请求的URL和发送的事件名直接映射到指定的Commander参数
     * @param namespace    命名空间
     * @param command      事件名称
     * @param commandClass 事件Commander参数类型列表
     */
    void addCommandMapping(String namespace, String command, List<Class<?>> commandClass);

    /**
     * 将字节流解析为对象
     *
     * @param  packet    数据包封装
     * @param  content   字节流
     * @param  valueType 目标对象类型
     * @return 解析后的对象
     */
    <T> T readValue(Packet packet, InputStream content, Class<T> valueType) throws Exception;

    /**
     * 将对象解析为字节流
     *
     * @param content 字节流
     * @param value   编码对象
     */
    void writeValue(OutputStream content, Object value) throws Exception;

    /**
     * 获取解析过程中产生的缓冲区数据列表，供外部进行序列化
     *
     * @return 缓冲区数据列表
     */
    List<byte[]> getBuffers();
}
