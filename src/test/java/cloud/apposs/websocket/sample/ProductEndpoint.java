package cloud.apposs.websocket.sample;

import cloud.apposs.ioc.annotation.Autowired;
import cloud.apposs.websocket.WSConfig;
import cloud.apposs.websocket.WSContextHolder;
import cloud.apposs.websocket.WSSession;
import cloud.apposs.websocket.annotation.OnCommand;
import cloud.apposs.websocket.annotation.OnConnect;
import cloud.apposs.websocket.annotation.ServerEndpoint;

@ServerEndpoint("/product")
public class ProductEndpoint {
    private final WSConfig config;

    private WSContextHolder holder;

    public ProductEndpoint(WSConfig config, WSContextHolder holder) {
        this.config = config;
        this.holder = holder;
    }

    @OnConnect
    public void onConnect(WSSession session) {
        System.out.println("product connected");
    }

    @OnCommand("context")
    public void onCommand01(WSSession session) throws Exception {
        session.sendCommand("event_send03", "Current WS Context: " + holder.getConfiguration());
    }
}
