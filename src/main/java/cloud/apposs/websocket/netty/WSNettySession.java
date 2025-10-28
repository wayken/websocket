package cloud.apposs.websocket.netty;

import cloud.apposs.websocket.WSConfig;
import cloud.apposs.websocket.WSSession;
import cloud.apposs.websocket.WSSessionBox;
import cloud.apposs.websocket.namespace.Namespace;
import cloud.apposs.websocket.protocol.HandshakeData;
import cloud.apposs.websocket.protocol.Packet;
import cloud.apposs.websocket.protocol.PacketType;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;

import java.util.UUID;

/**
 * 基于Netty的WebSocket会话
 */
public class WSNettySession extends WSSession  {
    private final ChannelHandlerContext context;

    public WSNettySession(UUID sessionId, String path, WSConfig configuration, Namespace namespace,
                          WSSessionBox sessionBox, HandshakeData handshakeData, ChannelHandlerContext context) {
        super(sessionId, path, configuration, namespace, sessionBox, handshakeData);
        this.context = context;
    }

    @Override
    public void handlePacketSend(byte[] packet) {
        context.channel().writeAndFlush(packet);
    }

    @Override
    public void handleChannelDisconnect() {
        Packet packet = new Packet(PacketType.DISCONNECT);
        ChannelFuture future = context.channel().writeAndFlush(packet);
        if (future != null) {
            future.addListener(ChannelFutureListener.CLOSE);
        }
    }
}
