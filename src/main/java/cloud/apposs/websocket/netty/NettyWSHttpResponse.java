package cloud.apposs.websocket.netty;

import cloud.apposs.websocket.WSHttpResponse;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 基于Netty ChannelHandlerContext的WSHttpResponse实现
 */
public class NettyWSHttpResponse implements WSHttpResponse {
    private final ChannelHandlerContext context;
    private final boolean keepAlive;
    private HttpResponseStatus status = HttpResponseStatus.OK;
    private final HttpHeaders headers = new DefaultHttpHeaders();

    public NettyWSHttpResponse(ChannelHandlerContext context, boolean keepAlive) {
        this.context = context;
        this.keepAlive = keepAlive;
    }

    @Override
    public void setStatus(int statusCode) {
        this.status = HttpResponseStatus.valueOf(statusCode);
    }

    @Override
    public void putHeader(String key, String value) {
        headers.set(key, value);
    }

    @Override
    public void setContentType(String contentType) {
        headers.set(HttpHeaderNames.CONTENT_TYPE, contentType);
    }

    @Override
    public void write(String content, boolean flush) throws IOException {
        write(content.getBytes(StandardCharsets.UTF_8), flush);
    }

    @Override
    public void write(byte[] content, boolean flush) throws IOException {
        DefaultFullHttpResponse response = new DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1, status, Unpooled.wrappedBuffer(content));
        response.headers().add(headers);
        response.headers().set(HttpHeaderNames.CONTENT_LENGTH, content.length);
        if (keepAlive) {
            response.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
        }
        if (flush) {
            if (keepAlive) {
                context.writeAndFlush(response);
            } else {
                context.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
            }
        } else {
            context.write(response);
        }
    }

    @Override
    public void flush() throws IOException {
        context.flush();
    }
}
