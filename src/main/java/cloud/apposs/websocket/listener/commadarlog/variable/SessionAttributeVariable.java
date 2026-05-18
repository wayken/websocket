package cloud.apposs.websocket.listener.commadarlog.variable;

import cloud.apposs.websocket.WSSession;
import cloud.apposs.websocket.commandar.Commandar;

public class SessionAttributeVariable implements IVariable {
    private final String attribute;

    public SessionAttributeVariable(String attribute) {
        this.attribute = attribute;
    }

    @Override
    public String parse(Commandar commandar, WSSession session, Throwable cause) {
        Object value = session.getAttribute(attribute);
        if (value == null) {
            return "-";
        }
        return value.toString();
    }
}
