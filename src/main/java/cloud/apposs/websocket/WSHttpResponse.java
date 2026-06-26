package cloud.apposs.websocket;

import java.io.IOException;

/**
 * HTTP响应抽象接口，屏蔽底层网络框架的具体实现
 */
public interface WSHttpResponse {
    /**
     * 获取响应状态码
     */
    String getStatus();

    /**
     * 设置响应状态码
     */
    void setStatus(int status);

    /**
     * 设置响应头
     */
    void putHeader(String key, String value);

    /**
     * 设置Content-Type
     */
    void setContentType(String contentType);

    /**
     * 响应字符串
     */
    void write(String content, boolean flush) throws IOException;

    /**
     * 响应字节数据
     */
    void write(byte[] content, boolean flush) throws IOException;

    /**
     * 刷新输出
     */
    void flush() throws IOException;
}
