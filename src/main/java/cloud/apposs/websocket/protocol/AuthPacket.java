package cloud.apposs.websocket.protocol;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;

public class AuthPacket {
    private final UUID sid;

    private final Map<String, String> parameter;

    public AuthPacket(UUID sid, Map<String, String> parameter) {
        super();
        this.sid = sid;
        this.parameter = Collections.unmodifiableMap(parameter);
    }

    public UUID getSid() {
        return sid;
    }

    public Map<String, String> getParameter() {
        return parameter;
    }
}
