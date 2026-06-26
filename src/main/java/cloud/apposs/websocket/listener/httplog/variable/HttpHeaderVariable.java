package cloud.apposs.websocket.listener.httplog.variable;

import cloud.apposs.rest.Handler;
import cloud.apposs.util.StrUtil;
import cloud.apposs.websocket.WSHttpRequest;
import cloud.apposs.websocket.WSHttpResponse;

/**
 * 请求头部，对应参数：$http_xxx_xxx
 */
public class HttpHeaderVariable extends AbstractVariable {
    private final String header;

    public HttpHeaderVariable(String header) {
        this.header = header;
    }

    @Override
    public String parse(WSHttpRequest request, WSHttpResponse response, Handler handler, Throwable t) {
        String value = request.getHeader(header);
        if (StrUtil.isEmpty(value)) {
            return "-";
        }
        return value;
    }
}
