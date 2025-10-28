package cloud.apposs.websocket.netty;

import cloud.apposs.websocket.WSConfig;
import cloud.apposs.websocket.WSSessionBox;
import cloud.apposs.websocket.scheduler.CancelableScheduler;
import cloud.apposs.websocket.scheduler.HashedWheelTimeoutScheduler;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.websocketx.extensions.compression.WebSocketServerCompressionHandler;
import io.netty.handler.ssl.SslHandler;

import javax.net.ssl.*;
import java.security.KeyStore;

/**
 * SocketIO服务端通道初始化
 */
public class SocketIOChannelInitializer extends ChannelInitializer<Channel>  {
    public static final String SSL_HANDLER = "ssl";
    public static final String AUTHORIZE_HANDLER = "authorizeHandler";
    public static final String HTTP_REQUEST_DECODER = "httpDecoder";
    public static final String HTTP_ENCODER = "httpEncoder";
    public static final String HTTP_AGGREGATOR = "httpAggregator";
    public static final String HTTP_COMPRESSION = "httpCompression";
    public static final String WEB_SOCKET_TRANSPORT_COMPRESSION = "webSocketTransportCompression";
    public static final String WEB_SOCKET_TRANSPORT = "webSocketTransport";
    public static final String WEB_SOCKET_AGGREGATOR = "webSocketAggregator";
    public static final String PACKET_ENCODE_HANDLER = "packetEncodeHandler";

    private WSConfig configuration;

    private AuthorizeHandler authorizeHandler;

    private WebSocketHandler webSocketHandler;

    private PacketEncodeHandler packetEncodeHandler;

    private SSLContext sslContext;

    private CancelableScheduler scheduler = new HashedWheelTimeoutScheduler();

    private final WebSocketContextHolder contextHolder;

    private final WSSessionBox sessionBox;

    public SocketIOChannelInitializer(WebSocketContextHolder contextHolder, WSSessionBox sessionBox) {
        this.contextHolder = contextHolder;
        this.sessionBox = sessionBox;
    }

    public SocketIOChannelInitializer initialize(WSConfig configuration) throws Exception {
        this.configuration = configuration;
        boolean isSsl = configuration.getKeyStore() != null;
        if (isSsl) {
            try {
                sslContext = createSSLContext(configuration);
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }

        this.authorizeHandler = new AuthorizeHandler(configuration, scheduler, contextHolder, sessionBox);
        this.webSocketHandler = new WebSocketHandler(configuration, contextHolder);
        this.packetEncodeHandler = new PacketEncodeHandler();
        return this;
    }

    @Override
    protected void initChannel(Channel ch) throws Exception {
        ChannelPipeline pipeline = ch.pipeline();
        addSslHandler(pipeline);
        addSocketioHandlers(pipeline);
    }

    private SSLContext createSSLContext(WSConfig configuration) throws Exception {
        TrustManager[] managers = null;
        if (configuration.getTrustStore() != null) {
            KeyStore ts = KeyStore.getInstance(configuration.getTrustStoreFormat());
            ts.load(configuration.getTrustStore(), configuration.getTrustStorePassword().toCharArray());
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init(ts);
            managers = tmf.getTrustManagers();
        }

        KeyStore ks = KeyStore.getInstance(configuration.getKeyStoreFormat());
        ks.load(configuration.getKeyStore(), configuration.getKeyStorePassword().toCharArray());

        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(ks, configuration.getKeyStorePassword().toCharArray());

        SSLContext serverContext = SSLContext.getInstance(configuration.getSslProtocol());
        serverContext.init(kmf.getKeyManagers(), managers, null);
        return serverContext;
    }

    private void addSslHandler(ChannelPipeline pipeline) {
        if (sslContext != null) {
            SSLEngine engine = sslContext.createSSLEngine();
            engine.setUseClientMode(false);
            if (configuration.isNeedClientAuth() &&(configuration.getTrustStore() != null)) {
                engine.setNeedClientAuth(true);
            }
            pipeline.addLast(SSL_HANDLER, new SslHandler(engine));
        }
    }

    protected void addSocketioHandlers(ChannelPipeline pipeline) {
        pipeline.addLast(HTTP_REQUEST_DECODER, new HttpRequestDecoder());
        pipeline.addLast(HTTP_AGGREGATOR, new HttpObjectAggregator(configuration.getMaxHttpContentLength()) {
            @Override
            protected Object newContinueResponse(HttpMessage start, int maxContentLength, ChannelPipeline pipeline) {
                return null;
            }
        });
        pipeline.addLast(HTTP_ENCODER, new HttpResponseEncoder());
        if (configuration.isHttpCompression()) {
            pipeline.addLast(HTTP_COMPRESSION, new HttpContentCompressor());
        }
        pipeline.addLast(AUTHORIZE_HANDLER, authorizeHandler);
        if (configuration.isWebsocketCompression()) {
            pipeline.addLast(WEB_SOCKET_TRANSPORT_COMPRESSION, new WebSocketServerCompressionHandler());
        }
        pipeline.addLast(PACKET_ENCODE_HANDLER, packetEncodeHandler);
        pipeline.addLast(WEB_SOCKET_TRANSPORT, webSocketHandler);
    }
}
