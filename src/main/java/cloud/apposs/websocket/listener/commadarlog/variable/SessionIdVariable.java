package cloud.apposs.websocket.listener.commadarlog.variable;

import cloud.apposs.websocket.WSSession;
import cloud.apposs.websocket.commandar.Commandar;

public class SessionIdVariable implements IVariable {
    @Override
    public String parse(Commandar commandar, WSSession session, Throwable cause) {
        return session.getSessionId().toString();
    }
}
