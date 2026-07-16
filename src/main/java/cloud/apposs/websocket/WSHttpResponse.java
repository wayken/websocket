package cloud.apposs.websocket;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

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
     * Writes a file response. Network implementations can override this method
     * to use an operating-system zero-copy transfer.
     */
    default void write(File file, boolean flush) throws IOException {
        write(Files.readAllBytes(file.toPath()), flush);
    }

    /**
     * 刷新输出
     */
    void flush() throws IOException;
}
