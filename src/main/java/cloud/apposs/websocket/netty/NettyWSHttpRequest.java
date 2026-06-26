package cloud.apposs.websocket.netty;

import cloud.apposs.util.JsonUtil;
import cloud.apposs.util.MediaType;
import cloud.apposs.util.Param;
import cloud.apposs.websocket.WSHttpRequest;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.handler.codec.http.multipart.Attribute;
import io.netty.handler.codec.http.multipart.DefaultHttpDataFactory;
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder;
import io.netty.handler.codec.http.multipart.InterfaceHttpData;

import java.io.IOException;
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
    private final FullHttpRequest request;

    private String path;

    private final SocketAddress remoteAddr;

    /**
     * 表单字段数据，支持GET/POST/FORM-URL/FORM-DATA-FIELD
     */
    private Map<String, String> parameters;

    /**
     * 表单JOSN数据，
     * application/json 类型的请求比较特殊，数据是一个JSON对象，所以无法单纯用parameters来储存
     */
    private Param bodyParam;


    private final Map<String, Object> attributes = new ConcurrentHashMap<>();

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
            if (request.method() == HttpMethod.GET) {
                // URL 参数数据传递
                QueryStringDecoder decoder = new QueryStringDecoder(request.uri());
                Map<String, List<String>> paramList = decoder.parameters();
                for(Map.Entry<String, List<String>> entry : paramList.entrySet()) {
                    parameters.put(entry.getKey(), entry.getValue().get(0));
                }
            }
            String contentType = request.headers().get(HttpHeaderNames.CONTENT_TYPE);
            if (MediaType.APPLICATION_FORM_URLENCODED.match(contentType)) {
                // POST URL 表单数据提交
                HttpPostRequestDecoder decoder = new HttpPostRequestDecoder(new DefaultHttpDataFactory(false), request);
                List<InterfaceHttpData> parmList = decoder.getBodyHttpDatas();
                for (InterfaceHttpData data : parmList) {
                    if (data.getHttpDataType() == InterfaceHttpData.HttpDataType.Attribute) {
                        Attribute attribute = (Attribute) data;
                        try {
                            parameters.put(attribute.getName(), attribute.getValue());
                        } catch (IOException e) {
                        }
                    }
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
                        Param param = JsonUtil.parseJsonParam(body);
                        if (param != null) {
                            bodyParam.putAll(param);
                        }
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
