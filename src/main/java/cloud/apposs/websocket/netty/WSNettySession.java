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

    public WSNettySession(
            UUID sessionId,
            String path,
            WSConfig configuration,
            Namespace namespace,
            WSSessionBox sessionBox,
            HandshakeData handshakeData,
            ChannelHandlerContext context,
            WebSocketContextHolder contextHolder
    ) {
        super(sessionId, path, configuration, namespace, sessionBox, handshakeData, contextHolder);
        this.context = context;
    }

    @Override
    public boolean isChannelOpen() {
        return context.channel().isActive();
    }

    @Override
    public boolean handlePacketSend(byte[] packet) {
        return context.channel().writeAndFlush(packet) != null;
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
