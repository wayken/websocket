package cloud.apposs.websocket.listener.commadarlog.variable;

import cloud.apposs.websocket.WSSession;
import cloud.apposs.websocket.WebSocketConstants;
import cloud.apposs.websocket.commandar.Commandar;

public class TimeVariable implements IVariable {
    @Override
    public String parse(Commandar commandar, WSSession session, Throwable cause) {
        Object attrValue = session.getAttribute(WebSocketConstants.COMMAND_ATTRIBUTE_START_TIME);
        // 在异步线程请求里面，有可能当前逻辑处理进来之前EventLoop可能因为请求超时或者异常先释放了请求，需要做空判断保护
        if (!(attrValue instanceof Long)) {
            return "0";
        }
        long startTime = (long) attrValue;
        return String.valueOf(System.currentTimeMillis() - startTime);
    }
}
