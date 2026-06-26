package cloud.apposs.websocket.listener.httplog.variable;

import cloud.apposs.rest.Handler;
import cloud.apposs.websocket.WSHttpRequest;
import cloud.apposs.websocket.WSHttpResponse;
import cloud.apposs.websocket.WebSocketConstants;

/**
 * 请求远程主机，对应参数：$host
 */
public class TimeVariable extends AbstractVariable {
    @Override
    public String parse(WSHttpRequest request, WSHttpResponse response, Handler handler, Throwable t) {
        Object attrValue = request.getAttribute(WebSocketConstants.REQUEST_ATTRIBUTE_START_TIME);
        // 在异步线程请求里面，有可能当前逻辑处理进来之前EventLoop可能因为请求超时或者异常先释放了请求，需要做空判断保护
        if (!(attrValue instanceof Long)) {
            return "0";
        }
        long startTime = (long) attrValue;
        return String.valueOf(System.currentTimeMillis() - startTime);
    }
}
