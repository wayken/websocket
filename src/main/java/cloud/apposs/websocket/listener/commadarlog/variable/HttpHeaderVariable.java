package cloud.apposs.websocket.listener.commadarlog.variable;

import cloud.apposs.util.StrUtil;
import cloud.apposs.websocket.WSSession;
import cloud.apposs.websocket.commandar.Commandar;

/**
 * 请求头部，对应参数：$http_xxx_xxx
 */
public class HttpHeaderVariable implements IVariable {
    private final String header;

    public HttpHeaderVariable(String header) {
        this.header = header;
    }

    @Override
    public String parse(Commandar commandar, WSSession session, Throwable cause) {
        String value = session.getHeader(header);
        if (StrUtil.isEmpty(value)) {
            return "-";
        }
        return value;
    }
}
