package cloud.apposs.websocket.netty;

import cloud.apposs.util.Param;
import cloud.apposs.websocket.WSHttpRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.QueryStringDecoder;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 基于Netty FullHttpRequest的WSHttpRequest实现
 */
public class NettyWSHttpRequest implements WSHttpRequest {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final FullHttpRequest request;
    private final SocketAddress remoteAddr;
    private final Map<String, Object> attributes = new ConcurrentHashMap<>();
    private Map<String, String> parameters;
    private Param bodyParam;
    private String path;

    public NettyWSHttpRequest(FullHttpRequest request, SocketAddress remoteAddr) {
        this.request = request;
        this.remoteAddr = remoteAddr;
    }

    @Override
    public SocketAddress getRemoteAddr() {
        return remoteAddr;
    }

    @Override
    public String getRemoteHost() {
        if (remoteAddr instanceof InetSocketAddress) {
            return ((InetSocketAddress) remoteAddr).getAddress().getHostAddress();
        }
        return remoteAddr != null ? remoteAddr.toString() : null;
    }

    @Override
    public String getPath() {
        if (path == null) {
            path = new QueryStringDecoder(request.uri()).path();
        }
        return path;
    }

    @Override
    public String getUri() {
        return request.uri();
    }

    @Override
    public String getMethod() {
        return request.method().name();
    }

    @Override
    public String getHost() {
        String host = request.headers().get(HttpHeaderNames.HOST);
        if (host != null) {
            int idx = host.indexOf(":");
            if (idx > 0) {
                return host.substring(0, idx);
            }
        }
        return host != null ? host : "*";
    }

    @Override
    public String getHeader(String key) {
        return request.headers().get(key);
    }

    @Override
    public Map<String, String> getHeaders() {
        Map<String, String> map = new HashMap<>();
        for (Map.Entry<String, String> entry : request.headers()) {
            map.put(entry.getKey(), entry.getValue());
        }
        return map;
    }

    @Override
    public Map<String, String> getParameters() {
        if (parameters == null) {
            parameters = new HashMap<>();
            QueryStringDecoder decoder = new QueryStringDecoder(request.uri());
            for (Map.Entry<String, List<String>> entry : decoder.parameters().entrySet()) {
                List<String> values = entry.getValue();
                if (values != null && !values.isEmpty()) {
                    parameters.put(entry.getKey(), values.get(values.size() - 1));
                }
            }
        }
        return parameters;
    }

    @Override
    public String getParameter(String key) {
        return getParameters().get(key);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Param getBodyParam() {
        if (bodyParam == null) {
            bodyParam = new Param();
            if (request.content().readableBytes() > 0) {
                String contentType = request.headers().get(HttpHeaderNames.CONTENT_TYPE);
                String body = request.content().toString(StandardCharsets.UTF_8);
                try {
                    if (contentType != null && contentType.contains("application/json")) {
                        Map<String, Object> json = MAPPER.readValue(body, Map.class);
                        bodyParam.putAll(json);
                    } else {
                        QueryStringDecoder bodyDecoder = new QueryStringDecoder("?" + body);
                        for (Map.Entry<String, List<String>> entry : bodyDecoder.parameters().entrySet()) {
                            List<String> values = entry.getValue();
                            if (values != null && !values.isEmpty()) {
                                bodyParam.put(entry.getKey(), values.get(values.size() - 1));
                            }
                        }
                    }
                } catch (Exception ignored) {
                }
            }
        }
        return bodyParam;
    }

    @Override
    public Object getAttribute(String key) {
        return attributes.get(key);
    }

    @Override
    public void setAttribute(String key, Object value) {
        attributes.put(key, value);
    }

    /**
     * 获取原始Netty请求对象（框架内部使用）
     */
    public FullHttpRequest getRawRequest() {
        return request;
    }
}
