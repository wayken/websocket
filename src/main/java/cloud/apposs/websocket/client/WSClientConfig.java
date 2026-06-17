package cloud.apposs.websocket.client;

import cloud.apposs.websocket.protocol.JacksonJsonSupport;
import cloud.apposs.websocket.protocol.JsonSupport;

/**
 * WebSocket客户端配置
 */
public class WSClientConfig {
    private String host = "127.0.0.1";
    private int port = 7010;
    private String path = "/socket.io";
    private String charset = "utf-8";

    // 是否自动重连
    private boolean reconnectOn = true;
    // 最大重连次数，-1表示不限制
    private int maxReconnectAttempts = 5;
    // 重连间隔（毫秒）
    private int reconnectInterval = 3000;

    // SSL相关
    private String sslProtocol;

    // JSON序列化支持
    private JsonSupport jsonSupport = new JacksonJsonSupport();

    // 连接超时（毫秒）
    private int connectTimeout = 5000;

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getCharset() {
        return charset;
    }

    public void setCharset(String charset) {
        this.charset = charset;
    }

    public boolean isReconnectOn() {
        return reconnectOn;
    }

    public void setReconnectOn(boolean reconnectOn) {
        this.reconnectOn = reconnectOn;
    }

    public int getMaxReconnectAttempts() {
        return maxReconnectAttempts;
    }

    public void setMaxReconnectAttempts(int maxReconnectAttempts) {
        this.maxReconnectAttempts = maxReconnectAttempts;
    }

    public int getReconnectInterval() {
        return reconnectInterval;
    }

    public void setReconnectInterval(int reconnectInterval) {
        this.reconnectInterval = reconnectInterval;
    }

    public String getSslProtocol() {
        return sslProtocol;
    }

    public void setSslProtocol(String sslProtocol) {
        this.sslProtocol = sslProtocol;
    }

    public JsonSupport getJsonSupport() {
        return jsonSupport;
    }

    public void setJsonSupport(JsonSupport jsonSupport) {
        this.jsonSupport = jsonSupport;
    }

    public int getConnectTimeout() {
        return connectTimeout;
    }

    public void setConnectTimeout(int connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    public String getUri() {
        String scheme = sslProtocol != null ? "wss" : "ws";
        return scheme + "://" + host + ":" + port + path;
    }
}
