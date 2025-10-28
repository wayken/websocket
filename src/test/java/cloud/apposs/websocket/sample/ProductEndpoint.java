package cloud.apposs.websocket.sample;

import cloud.apposs.ioc.annotation.Autowired;
import cloud.apposs.websocket.WSConfig;
import cloud.apposs.websocket.WSSession;
import cloud.apposs.websocket.annotation.OnConnect;
import cloud.apposs.websocket.annotation.ServerEndpoint;

@ServerEndpoint("/product")
public class ProductEndpoint {
    private final WSConfig config;

    @Autowired
    public ProductEndpoint(WSConfig config) {
        this.config = config;
    }

    @OnConnect
    public void onConnect(WSSession session) {
        System.out.println("product connected");
    }
}
