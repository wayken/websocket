package cloud.apposs.websocket.listener.httplog.variable;

import cloud.apposs.rest.Handler;
import cloud.apposs.rest.annotation.Request;
import cloud.apposs.websocket.WSHttpRequest;
import cloud.apposs.websocket.WSHttpResponse;

/**
 * 请求远程方法，对应参数：$method
 */
public class RequestMethodVariable extends AbstractVariable {
    @Override
    public String parse(WSHttpRequest request, WSHttpResponse response, Handler handler, Throwable t) {
        if (handler != null && handler.hasAnnotation(Request.Read.class)) {
            return "READ";
        } else if (handler != null && handler.hasAnnotation(Request.Post.class)) {
            return "POST";
        }
        return request.getMethod().toUpperCase();
    }
}
