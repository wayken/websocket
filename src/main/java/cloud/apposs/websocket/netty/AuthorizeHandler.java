package cloud.apposs.websocket.netty;

import cloud.apposs.logger.Logger;
import cloud.apposs.websocket.WSConfig;
import cloud.apposs.websocket.WSSession;
import cloud.apposs.websocket.WSSessionBox;
import cloud.apposs.websocket.interceptor.CommandarInterceptorSupport;
import cloud.apposs.websocket.namespace.Namespace;
import cloud.apposs.websocket.protocol.HandshakeData;
import cloud.apposs.websocket.scheduler.CancelableScheduler;
import cloud.apposs.websocket.scheduler.SchedulerKey;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.*;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

@Sharable
public class AuthorizeHandler extends ChannelInboundHandlerAdapter {
    private final WSConfig configuration;

    private final CancelableScheduler scheduler;

    private final WebSocketContextHolder contextHolder;

    private final WSSessionBox sessionBox;

    public AuthorizeHandler(WSConfig configuration, CancelableScheduler scheduler,
                            WebSocketContextHolder contextHolder, WSSessionBox sessionBox) {
        this.configuration = configuration;
        this.scheduler = scheduler;
        this.contextHolder = contextHolder;
        this.sessionBox = sessionBox;
    }

    @Override
    public void channelActive(final ChannelHandlerContext context) throws Exception {
        // 定期检测客户端有没有进行WebSocket握手通讯，避免恶意空连接
        SchedulerKey key = new SchedulerKey(SchedulerKey.Type.PING_TIMEOUT, context.channel());
        scheduler.schedule(key, () -> {
            context.channel().close();
            if (Logger.isDebugEnabled()) {
                Logger.debug("Client with ip %s opened channel but doesn't send any data! Channel closed!",
                        context.channel().remoteAddress());
            }
        }, configuration.getFirstDataTimeout(), TimeUnit.MILLISECONDS);
        super.channelActive(context);
    }

    @Override
    public void channelRead(ChannelHandlerContext context, Object message) throws Exception {
        SchedulerKey key = new SchedulerKey(SchedulerKey.Type.PING_TIMEOUT, context.channel());
        scheduler.cancel(key);

        if (message instanceof FullHttpRequest) {
            FullHttpRequest request = (FullHttpRequest) message;
            Channel channel = context.channel();
            QueryStringDecoder queryDecoder = new QueryStringDecoder(request.uri());
            String path = queryDecoder.path();
            if (!path.endsWith("/")) {
                path = path + "/";
            }

            // 如果没有找到对应的命名空间则表示业务没有注释@ServerEndpoint对应的命名空间，拒绝连接
            if (!contextHolder.getNamespacesHub().contains(path)) {
                HttpResponse response = new DefaultHttpResponse(HTTP_1_1, HttpResponseStatus.BAD_REQUEST);
                channel.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
                request.release();
                Logger.warn("No namespace %s found for request %s", path, request.uri());
                return;
            }

            if (!authorize(context, queryDecoder, path, request)) {
                request.release();
                return;
            }
        }
        context.fireChannelRead(message);
    }

    private boolean authorize(ChannelHandlerContext context, QueryStringDecoder decoder, String path, FullHttpRequest request) throws Exception {
        Map<String, List<String>> headers = new HashMap<String, List<String>>(request.headers().names().size());
        Map<String, List<String>> parameters = decoder.parameters();
        for (String name : request.headers().names()) {
            List<String> values = request.headers().getAll(name);
            headers.put(name, values);
        }

        Channel channel = context.channel();
        // 调用业务认证接口进行WebSocket拦截认证，因为Parameters是一个List，所以只取最后一个参数值
        Map<String, String> formatParameters = new HashMap<String, String>();
        for (Map.Entry<String, List<String>> entry : parameters.entrySet()) {
            String key = entry.getKey();
            List<String> values = entry.getValue();
            if (values != null && !values.isEmpty()) {
                formatParameters.put(key, values.get(values.size() - 1));
            }
        }
        HandshakeData handshakeData = new HandshakeData(formatParameters,
                (InetSocketAddress) channel.remoteAddress(),
                (InetSocketAddress) channel.localAddress(),
                request.uri());
        CommandarInterceptorSupport interceptorSupport = contextHolder.getCommandarInterceptorSupport();
        boolean isAuthSuccess = interceptorSupport.isAuthorized(handshakeData);
        if (!isAuthSuccess) {
            HttpResponse res = new DefaultHttpResponse(HTTP_1_1, HttpResponseStatus.UNAUTHORIZED);
            channel.writeAndFlush(res).addListener(ChannelFutureListener.CLOSE);
            Logger.warn("Handshake unauthorized, query params: %s headers: %s", parameters, headers);
            return false;
        }
        // 创建会话信息
        UUID sessionId = UUID.randomUUID();
        Namespace namespace = contextHolder.getNamespacesHub().get(path);
        WSSession session = new WSNettySession(sessionId, path, configuration,
                namespace, sessionBox, handshakeData, context);
        channel.attr(ChannelAttributeKey.SESSION).set(session);
        return true;
    }
}
