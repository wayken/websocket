package cloud.apposs.websocket.sample.service;

import cloud.apposs.ioc.annotation.Component;
import cloud.apposs.websocket.WSSession;

@Component
public class UserService {
    public void chat(WSSession session) throws Exception {
        session.sendCommand("chat", "Hello, World!");
    }
}
