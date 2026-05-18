package cloud.apposs.websocket.listener.commadarlog.variable;

import cloud.apposs.websocket.WSSession;
import cloud.apposs.websocket.commandar.Commandar;

public class LiteralVariable implements IVariable {
    private final String literal;

    public LiteralVariable(String literal) {
        this.literal = literal;
    }

    @Override
    public String parse(Commandar commandar, WSSession session, Throwable cause) {
        return literal;
    }
}
