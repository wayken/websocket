package cloud.apposs.websocket.sample;

import cloud.apposs.websocket.WSSession;
import cloud.apposs.websocket.annotation.*;
import cloud.apposs.websocket.sample.bean.ChatObject;
import cloud.apposs.websocket.sample.bean.ChatUsers;

import java.util.Map;
import java.util.UUID;

@ServerEndpoint("/socket.io")
public class SocketIOEndpoint {
    public static final int CMD_EVENT_01 = 100;

    @OnConnect
    public void onConnect(WSSession session) {
        System.out.println("user connected");
    }

    @OnEvent(CMD_EVENT_01)
    public void onEvent01(WSSession session, String message) throws Exception {
        System.out.println("onEvent01 " + message);
        session.sendEvent((short) 100, message + " from Server");
    }

    @OnEvent(101)
    public void onEvent02(ChatObject message) {
        System.out.println("onEvent02 " + message.getUsername());
    }

    @OnEvent(102)
    public void onEvent03(WSSession session, ChatUsers users) throws Exception {
        session.disconnect();
        System.out.println("onEvent03 " + users.getUsers());
    }

    @OnEvent(103)
    public void onEvent04(WSSession session, byte[] data) throws Exception {
        session.disconnect();
        System.out.println("onEvent04 " + data.length);
    }

    @OnEvent(104)
    public void onEvent05(WSSession session, ChatObject data) throws Exception {
        System.out.println("onEvent03");
        data.setMessage("from server0");
        // 分发在集群中所有连接的客户端
        Map<UUID, String> sessionList = session.getNamespace().getDistributedSessions();
        for (UUID sessionId : sessionList.keySet()) {
            if (sessionId.equals(session.getSessionId())) {
                continue;
            }
            ChatObject replyData = new ChatObject();
            replyData.setUsername(data.getUsername());
            replyData.setMessage("reply to " + sessionId);
            session.getDistributedSessionOperations(sessionId).sendEvent((short) 101, replyData);
        }
        System.out.println(sessionList);
    }

    @OnError
    public void onError(Throwable ex) {
        System.out.println("exp caught " + ex.getMessage());
        ex.printStackTrace();
    }

    @OnDisconnect
    public void onDisconnect(WSSession session) {
        System.err.println("disconnected");
    }
}
