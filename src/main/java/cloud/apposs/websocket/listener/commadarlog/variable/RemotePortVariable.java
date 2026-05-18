package cloud.apposs.websocket.listener.commadarlog.variable;

import cloud.apposs.websocket.WSSession;
import cloud.apposs.websocket.commandar.Commandar;

public class RemotePortVariable implements IVariable {
    @Override
    public String parse(Commandar commandar, WSSession session, Throwable cause) {
        return String.valueOf(session.getRemoteAddress().getPort());
    }
}
