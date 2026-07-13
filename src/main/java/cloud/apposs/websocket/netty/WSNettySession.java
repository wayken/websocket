package cloud.apposs.websocket.netty;

import cloud.apposs.websocket.WSConfig;
import cloud.apposs.websocket.WSSession;
import cloud.apposs.websocket.WSSessionBox;
import cloud.apposs.websocket.namespace.Namespace;
import cloud.apposs.websocket.protocol.HandshakeData;
import cloud.apposs.websocket.protocol.Packet;
import cloud.apposs.websocket.protocol.PacketType;
import cloud.apposs.websocket.scheduler.CancelableScheduler;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;

import java.util.Map;
import java.util.UUID;

/**
 * 基于Netty的WebSocket会话
 */
public class WSNettySession extends WSSession  {
    private final ChannelHandlerContext channelHandlerContext;

    public WSNettySession(
            UUID sessionId,
            String path,
            WSConfig configuration,
            Namespace namespace,
            Map<String, String> headers,
            WSSessionBox sessionBox,
            HandshakeData handshakeData,
            ChannelHandlerContext channelHandlerContext,
            CancelableScheduler scheduler
    ) {
        super(sessionId, path, configuration, namespace, headers, sessionBox, handshakeData, scheduler);
        this.channelHandlerContext = channelHandlerContext;
    }

    @Override
    public boolean isChannelOpen() {
        return channelHandlerContext.channel().isActive();
    }

    @Override
    public boolean handlePacketSend(byte[] packet) {
        return channelHandlerContext.channel().writeAndFlush(packet) != null;
    }

    @Override
    public void handleChannelDisconnect() {
        Packet packet = new Packet(PacketType.DISCONNECT);
        ChannelFuture future = channelHandlerContext.channel().writeAndFlush(packet);
        if (future != null) {
            future.addListener(ChannelFutureListener.CLOSE);
        }
    }
}
