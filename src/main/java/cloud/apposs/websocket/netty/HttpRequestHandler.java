package cloud.apposs.websocket.netty;

import cloud.apposs.logger.Logger;
import cloud.apposs.rest.Handler;
import cloud.apposs.rest.Restful;
import cloud.apposs.websocket.WSHttpRequest;
import cloud.apposs.websocket.WSHttpResponse;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.*;

import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

/**
 * HTTP请求处理器，与WebSocket复用同一端口，
 * 通过判断Upgrade头来区分WebSocket升级请求和普通HTTP请求
 */
@Sharable
public class HttpRequestHandler extends ChannelInboundHandlerAdapter {
    private final Restful<WSHttpRequest, WSHttpResponse> restful;
    private final NettyHandlerProcess handlerProcess;

    public HttpRequestHandler(Restful<WSHttpRequest, WSHttpResponse> restful, NettyHandlerProcess handlerProcess) {
        this.restful = restful;
        this.handlerProcess = handlerProcess;
    }

    @Override
    public void channelRead(ChannelHandlerContext context, Object message) throws Exception {
        if (message instanceof FullHttpRequest) {
            FullHttpRequest request = (FullHttpRequest) message;
            String upgrade = request.headers().get(HttpHeaderNames.UPGRADE);
            if (upgrade != null && "websocket".equalsIgnoreCase(upgrade)) {
                context.fireChannelRead(message);
                return;
            }
            handleHttpRequest(context, request);
            return;
        }
        context.fireChannelRead(message);
    }

    private void handleHttpRequest(ChannelHandlerContext context, FullHttpRequest rawRequest) {
        boolean keepAlive = HttpUtil.isKeepAlive(rawRequest);
        WSHttpRequest request = new NettyWSHttpRequest(rawRequest, context.channel().remoteAddress());
        WSHttpResponse response = new NettyWSHttpResponse(context, keepAlive);

        Handler handler = restful.getHandler(handlerProcess, request, response);
        if (handler == null) {
            DefaultFullHttpResponse resp = new DefaultFullHttpResponse(
                    HTTP_1_1, HttpResponseStatus.NOT_FOUND,
                    Unpooled.copiedBuffer("Not Found".getBytes())
            );
            resp.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain; charset=UTF-8");
            resp.headers().set(HttpHeaderNames.CONTENT_LENGTH, resp.content().readableBytes());
            context.writeAndFlush(resp).addListener(ChannelFutureListener.CLOSE);
            rawRequest.release();
            return;
        }
        try {
            restful.renderView(handlerProcess, request, response);
        } catch (Exception e) {
            Logger.error(e, "Http request handle error: %s", rawRequest.uri());
            DefaultFullHttpResponse resp = new DefaultFullHttpResponse(
                    HTTP_1_1, HttpResponseStatus.INTERNAL_SERVER_ERROR,
                    Unpooled.copiedBuffer("Internal Server Error".getBytes())
            );
            resp.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain; charset=UTF-8");
            resp.headers().set(HttpHeaderNames.CONTENT_LENGTH, resp.content().readableBytes());
            context.writeAndFlush(resp).addListener(ChannelFutureListener.CLOSE);
            rawRequest.release();
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext context, Throwable cause) throws Exception {
        Logger.error(cause, "HttpRequestHandler error");
        context.close();
    }
}
