package cloud.apposs.websocket.listener.httplog.variable;

import cloud.apposs.rest.Handler;
import cloud.apposs.util.StrUtil;
import cloud.apposs.websocket.WSHttpRequest;
import cloud.apposs.websocket.WSHttpResponse;

/**
 * 请求内部属性获取，主要为内部系统设置，对应参数：$attr_xxx_xxx
 */
public class HttpAttributeVariable extends AbstractVariable {
    private final String attribute;

    public HttpAttributeVariable(String attribute) {
        this.attribute = attribute;
    }

    @Override
    public String parse(WSHttpRequest request, WSHttpResponse response, Handler handler, Throwable t) {
        Object value = request.getAttribute(attribute);
        if (StrUtil.isEmpty(value)) {
            return "-";
        }
        return value.toString();
    }
}
