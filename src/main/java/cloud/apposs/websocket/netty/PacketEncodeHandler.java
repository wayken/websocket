package cloud.apposs.websocket.netty;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;

import java.util.List;

/**
 * WebSocket数据包编码器
 */
@Sharable
public class PacketEncodeHandler extends MessageToMessageEncoder<byte[]> {
    @Override
    protected void encode(ChannelHandlerContext ctx, byte[] msg, List<Object> out) throws Exception {
        out.add(new BinaryWebSocketFrame(Unpooled.wrappedBuffer(msg)));
    }
}
