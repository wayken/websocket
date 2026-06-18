package cloud.apposs.websocket;

import cloud.apposs.util.Param;

import java.net.SocketAddress;
import java.util.Map;

/**
 * HTTP请求抽象接口，屏蔽底层网络框架（Netty/Undertow等）的具体实现
 */
public interface WSHttpRequest {
    /**
     * 获取远程地址
     */
    SocketAddress getRemoteAddr();

    /**
     * 获取请求路径（不含查询参数）
     */
    String getPath();

    /**
     * 获取完整URI
     */
    String getUri();

    /**
     * 获取请求方法（GET/POST/PUT/DELETE等）
     */
    String getMethod();

    /**
     * 获取请求Host
     */
    String getHost();

    /**
     * 获取指定Header值
     */
    String getHeader(String key);

    /**
     * 获取所有Header
     */
    Map<String, String> getHeaders();

    /**
     * 获取查询参数
     */
    Map<String, String> getParameters();

    /**
     * 获取指定查询参数值
     */
    String getParameter(String key);

    /**
     * 获取请求体JSON数据
     */
    Param getBodyParam();

    /**
     * 获取当前请求属性
     */
    Object getAttribute(String key);

    /**
     * 设置当前请求属性
     */
    void setAttribute(String key, Object value);
}
