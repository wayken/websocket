package cloud.apposs.websocket.netty;

import cloud.apposs.websocket.WSSession;
import cloud.apposs.websocket.protocol.PacketDecoder;
import io.netty.util.AttributeKey;

public final class ChannelAttributeKey {
    public static final AttributeKey<WSSession> SESSION = AttributeKey.<WSSession>valueOf("session");
    public static final AttributeKey<PacketDecoder> DECODER = AttributeKey.<PacketDecoder>valueOf("decoder");
}
