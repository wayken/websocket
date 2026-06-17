package cloud.apposs.websocket.client.netty;

import cloud.apposs.websocket.client.ClientPacketDecoder;
import cloud.apposs.websocket.client.WSClient;
import cloud.apposs.websocket.client.WSClientConfig;
import cloud.apposs.websocket.client.WSClientListener;
import cloud.apposs.websocket.protocol.Packet;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.websocketx.*;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;

import java.net.URI;
import java.nio.ByteBuffer;
import java.util.concurrent.TimeUnit;

/**
 * 基于Netty的WebSocket客户端实现
 */
public class NettyWSClient extends WSClient {
    private EventLoopGroup group;
    private Channel channel;
    private final ClientPacketDecoder decoder;

    public NettyWSClient(WSClientConfig config, WSClientListener listener) {
        super(config, listener);
        this.decoder = new ClientPacketDecoder(config.getJsonSupport(), config.getCharset());
    }

    @Override
    protected void handleConnect() throws Exception {
        group = new NioEventLoopGroup(1);
        URI uri = new URI(config.getUri());
        WebSocketClientHandshaker handshaker = WebSocketClientHandshakerFactory.newHandshaker(
                uri, WebSocketVersion.V13, null, true, new DefaultHttpHeaders());

        final NettyWSClientHandler handler = new NettyWSClientHandler(handshaker);

        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(group)
                .channel(NioSocketChannel.class)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, config.getConnectTimeout())
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        ChannelPipeline pipeline = ch.pipeline();
                        if (config.getSslProtocol() != null) {
                            SslContext sslContext = SslContextBuilder.forClient()
                                    .trustManager(InsecureTrustManagerFactory.INSTANCE)
                                    .protocols(config.getSslProtocol())
                                    .build();
                            pipeline.addLast(sslContext.newHandler(ch.alloc(), config.getHost(), config.getPort()));
                        }
                        pipeline.addLast(new HttpClientCodec());
                        pipeline.addLast(new HttpObjectAggregator(65536));
                        pipeline.addLast(new WebSocketFrameAggregator(65536));
                        pipeline.addLast(handler);
                    }
                });

        channel = bootstrap.connect(config.getHost(), config.getPort()).sync().channel();
        handler.handshakeFuture().sync();
    }

    @Override
    protected void handleDisconnect() {
        if (channel != null && channel.isActive()) {
            channel.writeAndFlush(new CloseWebSocketFrame()).syncUninterruptibly();
            channel.close().syncUninterruptibly();
        }
        channel = null;
    }

    @Override
    protected void handleShutdown() {
        if (group != null && !group.isShutdown()) {
            group.shutdownGracefully();
        }
    }

    @Override
    protected void handleSend(byte[] data) throws Exception {
        if (channel == null || !channel.isActive()) {
            throw new IllegalStateException("Channel is not active");
        }
        channel.writeAndFlush(new BinaryWebSocketFrame(Unpooled.wrappedBuffer(data)));
    }

    @Override
    protected void handleScheduleReconnect(int delayMs) {
        if (group != null && !group.isShutdown()) {
            group.schedule(() -> {
                try {
                    handleConnect();
                } catch (Exception e) {
                    onError(e);
                    onDisconnected();
                }
            }, delayMs, TimeUnit.MILLISECONDS);
        }
    }

    /**
     * Netty WebSocket客户端Handler，处理握手和消息
     */
    private class NettyWSClientHandler extends SimpleChannelInboundHandler<Object> {
        private final WebSocketClientHandshaker handshaker;
        private ChannelPromise handshakeFuture;

        NettyWSClientHandler(WebSocketClientHandshaker handshaker) {
            this.handshaker = handshaker;
        }

        ChannelFuture handshakeFuture() {
            return handshakeFuture;
        }

        @Override
        public void handlerAdded(ChannelHandlerContext ctx) {
            handshakeFuture = ctx.newPromise();
        }

        @Override
        public void channelActive(ChannelHandlerContext ctx) {
            handshaker.handshake(ctx.channel());
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) {
            onDisconnected();
        }

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
            if (!handshaker.isHandshakeComplete()) {
                handshaker.finishHandshake(ctx.channel(), (io.netty.handler.codec.http.FullHttpResponse) msg);
                handshakeFuture.setSuccess();
                onConnected();
                return;
            }
            if (msg instanceof BinaryWebSocketFrame) {
                ByteBuf buf = ((BinaryWebSocketFrame) msg).content();
                ByteBuffer nioBuffer = buf.nioBuffer();
                Packet packet = decoder.decode(nioBuffer);
                if (packet != null) {
                    onMessage(packet);
                }
            } else if (msg instanceof CloseWebSocketFrame) {
                channel.close();
            }
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            if (!handshakeFuture.isDone()) {
                handshakeFuture.setFailure(cause);
            }
            onError(cause);
            ctx.close();
        }
    }
}
