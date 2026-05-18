package cloud.apposs.websocket.listener.commadarlog.variable;

import cloud.apposs.websocket.WSSession;
import cloud.apposs.websocket.commandar.Commandar;

public class CommandPathVariable implements IVariable {
    @Override
    public String parse(Commandar commandar, WSSession session, Throwable cause) {
        if (commandar == null) {
            return "-";
        }
        return commandar.getPath().getPath();
    }
}
