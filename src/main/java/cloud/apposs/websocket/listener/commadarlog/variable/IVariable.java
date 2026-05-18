package cloud.apposs.websocket.listener.commadarlog.variable;

import cloud.apposs.websocket.WSSession;
import cloud.apposs.websocket.commandar.Commandar;

public interface IVariable {
    String parse(Commandar commandar, WSSession session, Throwable cause);
}
