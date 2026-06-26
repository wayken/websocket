package cloud.apposs.websocket.listener.httplog.variable;

import cloud.apposs.rest.Handler;
import cloud.apposs.websocket.WSHttpRequest;
import cloud.apposs.websocket.WSHttpResponse;

/**
 * 请求响应状态码，对应参数：$status
 */
public class HttpStatusVariable extends AbstractVariable {
    @Override
    public String parse(WSHttpRequest request, WSHttpResponse response, Handler handler, Throwable t) {
        return String.valueOf(response.getStatus());
    }
}
