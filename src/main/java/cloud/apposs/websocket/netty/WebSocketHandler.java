package cloud.apposs.websocket.netty;

import cloud.apposs.logger.Logger;
import cloud.apposs.util.Pair;
import cloud.apposs.websocket.WSConfig;
import cloud.apposs.websocket.WSSession;
import cloud.apposs.websocket.protocol.AuthPacket;
import cloud.apposs.websocket.protocol.Packet;
import cloud.apposs.websocket.protocol.PacketDecoder;
import cloud.apposs.websocket.protocol.PacketType;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufHolder;
import io.netty.channel.*;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.websocketx.*;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.UUID;

@Sharable
public class WebSocketHandler extends ChannelInboundHandlerAdapter {
    private final WSConfig configuration;

    private WebSocketServerHandshaker handshaker;

    private final WebSocketContextHolder contextHolder;

    public WebSocketHandler(WSConfig configuration, WebSocketContextHolder contextHolder) {
        this.configuration = configuration;
        this.contextHolder = contextHolder;
    }

    @Override
    public void channelRead(ChannelHandlerContext context, Object message) throws Exception {
        if (message instanceof FullHttpRequest) {
            final Channel channel = context.channel();
            final FullHttpRequest request = (FullHttpRequest) message;
            WebSocketServerHandshakerFactory factory = new WebSocketServerHandshakerFactory(
                    getWebSocketLocation(request), null, true, configuration.getMaxFramePayloadLength());
            handshaker = factory.newHandshaker(request);
            if (handshaker != null) {
                ChannelFuture f = handshaker.handshake(channel, request);
                f.addListener((ChannelFutureListener) future -> {
                    if (!future.isSuccess()) {
                        Logger.error("Can't handshake", future.cause());
                        return;
                    }
                    channel.pipeline().addBefore(SocketIOChannelInitializer.WEB_SOCKET_TRANSPORT, SocketIOChannelInitializer.WEB_SOCKET_AGGREGATOR,
                            new WebSocketFrameAggregator(configuration.getMaxFramePayloadLength()));
                    // 发送握手数据
                    WSSession session = context.channel().attr(ChannelAttributeKey.SESSION).get();
                    AuthPacket authPacket = new AuthPacket(session.getSessionId(), configuration.getHandshakeParameter(), configuration);
                    Packet packet = new Packet(PacketType.HANDSHAKE);
                    packet.setData(authPacket);
                    session.send(packet);
                    // 触发OnConnect事件
                    onConnect(context);
                });
            } else {
                WebSocketServerHandshakerFactory.sendUnsupportedVersionResponse(context.channel());
            }
            return;
        }
        if (message instanceof WebSocketFrame) {
            WebSocketFrame frame = (WebSocketFrame) message;
            // 关闭链路数据帧指令
            if (message instanceof CloseWebSocketFrame) {
                handshaker.close(context.channel(), (CloseWebSocketFrame) frame.retain());
                onDisConnect(context);
                return;
            }
            // 判断是否是 PING/PONG 消息
            if (message instanceof PingWebSocketFrame) {
                context.channel().writeAndFlush(new PongWebSocketFrame(frame.content().retain()));
                return;
            } else if (frame instanceof PongWebSocketFrame) {
                context.channel().writeAndFlush(new PingWebSocketFrame(frame.content().retain()));
                return;
            }
            // 接收并解码二进制数据包
            onMessage(context, (ByteBufHolder) message);
        }
        context.fireChannelRead(message);
    }

    @Override
    public void channelInactive(ChannelHandlerContext context) throws Exception {
        WSSession session = context.channel().attr(ChannelAttributeKey.SESSION).get();
        // 有可能是握手失败导致的连接关闭，此时没有 WSSession
        if (session != null) {
            session.onChannelDisconnect();
            try {
                contextHolder.onDisconnect(session);
            } catch (Throwable e) {
                contextHolder.onError(session.getPath(), e);
            }
        }
        super.channelInactive(context);
        final Channel channel = context.channel();
        channel.close();
        context.close();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext context, Throwable cause) throws Exception {
        // 如果是方法调用中有异常，需要获取的是真正的业务异常
        if (cause instanceof InvocationTargetException) {
            cause = ((InvocationTargetException) cause).getTargetException();
        }
        WSSession session = context.channel().attr(ChannelAttributeKey.SESSION).get();
        // 有可能是握手失败导致的连接关闭，此时没有 WSSession
        if (session != null) {
            if (!contextHolder.onError(session.getPath(), cause)) {
                context.fireExceptionCaught(cause);
            }
            return;
        }
        // 没有 WSSession 时，直接传递给下一个处理器处理异常
        context.fireExceptionCaught(cause);
    }

    public boolean onConnect(ChannelHandlerContext context) throws Exception {
        WSSession session = context.channel().attr(ChannelAttributeKey.SESSION).get();
        UUID sessionId = session.getSessionId();
        if (Logger.isDebugEnabled()) {
            Logger.debug("сlient %s handshake completed", sessionId);
        }
        try {
            contextHolder.onConnect(session);
        } catch (Throwable cause) {
            // 如果是方法调用中有异常，需要获取的是真正的业务异常
            if (cause instanceof InvocationTargetException) {
                cause = ((InvocationTargetException) cause).getTargetException();
            }
            if (!contextHolder.onError(session.getPath(), cause)) {
                throw (Exception) cause;
            }
        }
        return true;
    }

    public void onMessage(ChannelHandlerContext context, ByteBufHolder frame) throws Exception {
        WSSession session = context.channel().attr(ChannelAttributeKey.SESSION).get();
        PacketDecoder decoder = context.channel().attr(ChannelAttributeKey.DECODER).get();
        if (decoder == null) {
            decoder = new PacketDecoder(configuration.getJsonSupport(), configuration.getCharset());
            context.channel().attr(ChannelAttributeKey.DECODER).set(decoder);
        }
        ByteBuf buffer = frame.content();
        byte[] content = new byte[buffer.readableBytes()];
        buffer.readBytes(content);
        Pair<Boolean, Packet> packets = decoder.decode(session, content);
        if (packets == null) {
            throw new IOException("Decode packet error from client " + session.getSessionId());
        }
        // 如果数据包不完整则继续等待数据包
        boolean isPacketComplete = packets.key();
        if (!isPacketComplete) {
            return;
        }
        // 获取注解接口的 OnEvent 方法并执行方法回调
        Packet packet = packets.value();
        contextHolder.onEvent(session, packet);
    }

    public void onDisConnect(ChannelHandlerContext context) throws Exception {
        WSSession session = context.channel().attr(ChannelAttributeKey.SESSION).get();
        session.onChannelDisconnect();
    }

    private String getWebSocketLocation(HttpRequest req) {
        String protocol = "ws://";
        return protocol + req.headers().get(HttpHeaderNames.HOST) + req.uri();
    }
}
