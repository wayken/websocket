package cloud.apposs.websocket.listener.httplog.variable;

import cloud.apposs.rest.Handler;
import cloud.apposs.websocket.WSHttpRequest;
import cloud.apposs.websocket.WSHttpResponse;

/**
 * 异常消息解析，对应参数：$expm
 */
public class ExceptionMessageVariable extends AbstractVariable {
    @Override
    public String parse(WSHttpRequest request, WSHttpResponse response, Handler handler, Throwable t) {
        if (t == null) {
            return "-";
        }
        return t.getMessage();
    }
}
