package cloud.apposs.websocket.listener.commadarlog.variable;

import cloud.apposs.websocket.WSSession;
import cloud.apposs.websocket.commandar.Commandar;

public class ExceptionVariable implements IVariable {
    @Override
    public String parse(Commandar commandar, WSSession session, Throwable cause) {
        if (cause == null) {
            return "-";
        }
        return cause.toString();
    }
}
