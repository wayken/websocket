package cloud.apposs.websocket.netty;

import cloud.apposs.websocket.WSHttpResponse;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.DefaultFileRegion;
import io.netty.handler.codec.http.*;
import io.netty.handler.ssl.SslHandler;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
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
    public String getStatus() {
        return String.valueOf(status.code());
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
    public void write(File file, boolean flush) throws IOException {
        if (context.pipeline().get(SslHandler.class) != null) {
            write(Files.readAllBytes(file.toPath()), flush);
            return;
        }

        long length = file.length();
        DefaultHttpResponse response = new DefaultHttpResponse(HttpVersion.HTTP_1_1, status);
        response.headers().add(headers);
        response.headers().set(HttpHeaderNames.CONTENT_LENGTH, length);
        if (keepAlive) {
            response.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
        }

        // FileRegion must bypass content compression so the transport can use sendfile.
        ChannelHandlerContext writeContext = context.pipeline().context(HttpContentCompressor.class);
        if (writeContext == null) {
            writeContext = context;
        }
        writeContext.write(response);
        writeContext.write(new DefaultFileRegion(file, 0, length));
        ChannelFuture last = writeContext.write(LastHttpContent.EMPTY_LAST_CONTENT);
        if (!keepAlive) {
            last.addListener(ChannelFutureListener.CLOSE);
        }
        if (flush) {
            writeContext.flush();
        }
    }

    @Override
    public void flush() throws IOException {
        context.flush();
    }
}
