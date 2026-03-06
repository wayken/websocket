package cloud.apposs.websocket.protocol;

import cloud.apposs.websocket.WSConfig;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class AuthPacket {
    private static final String POLICY_RECONNECT_ON = "reconnect_on";
    private static final String POLICY_MAX_RECONNECT_ATTEMPTS = "max_reconnect_attempts";
    private static final String POLICY_RECONNECT_INTERVAL = "reconnect_interval";

    private final UUID sid;

    private final Map<String, Object> policy = new HashMap<>();

    private final Map<String, String> parameter;

    public AuthPacket(UUID sid, Map<String, String> parameter, WSConfig configuration) {
        super();
        this.sid = sid;
        this.policy.put(POLICY_RECONNECT_ON, configuration.isSocketReconnectOn());
        this.policy.put(POLICY_MAX_RECONNECT_ATTEMPTS, configuration.getSocketMaxReconnectAttempts());
        this.policy.put(POLICY_RECONNECT_INTERVAL, configuration.getSocketReconnectInterval());
        this.parameter = Collections.unmodifiableMap(parameter);
    }

    public UUID getSid() {
        return sid;
    }

    public Map<String, Object> getPolicy() {
        return policy;
    }

    public Map<String, String> getParameter() {
        return parameter;
    }
}
