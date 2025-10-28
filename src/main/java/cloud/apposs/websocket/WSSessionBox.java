package cloud.apposs.websocket;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class WSSessionBox {
    private final Map<UUID, WSSession> sessionBox = new ConcurrentHashMap<UUID, WSSession>();

    public WSSession getSession(UUID sessionId) {
        return sessionBox.get(sessionId);
    }

    public void addSession(WSSession session) {
        sessionBox.put(session.getSessionId(), session);
    }

    public void removeSession(UUID sessionId) {
        sessionBox.remove(sessionId);
    }
}
