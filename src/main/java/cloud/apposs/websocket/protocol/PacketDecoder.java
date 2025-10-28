package cloud.apposs.websocket.protocol;

import cloud.apposs.util.Pair;
import cloud.apposs.websocket.WSSession;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * WebSocket 二进制数据包解码器，
 * 每个Channel对应一个解码器，数据包格式详见{@link Packet}
 */
public class PacketDecoder {
    private final Packet packet;

    private final JsonSupport jsonSupport;

    private final String charset;

    public static final Pair<Boolean, Packet> FALSE = new Pair<Boolean, Packet>(false, null);

    public PacketDecoder(JsonSupport jsonSupport, String charset) {
        this.packet = new Packet();
        this.jsonSupport = jsonSupport;
        this.charset = charset;
    }

    /**
     * 解码数据包，数据包格式详见{@link Packet}
     *
     * @param  session 会话
     * @param  buffer  数据包字节流
     * @return 返回解码后的数据包，
     *  如果返回Pair.key为false表示数据包还没解包完整，需要等待下一次数据包，如果返回null则表示解码失败，
     *  如果返回Pair.key为true表示数据包解码完整，Pair.value为解码后的数据包
     */
    public Pair<Boolean, Packet> decode(WSSession session, byte[] buffer) throws Exception {
        if (buffer == null) {
            return null;
        }

        Packet.Head head = packet.getHead();
        ByteBuffer headBuf = head.getHeadBuf();
        // 接收数据还不完整，先缓存下来
        if (headBuf.remaining() > buffer.length) {
            headBuf.put(buffer);
            return FALSE;
        }
        int readHeadLen = Packet.Head.HEADER_LEN - headBuf.position();
        int position = headBuf.position();
        headBuf.put(buffer, 0, Packet.Head.HEADER_LEN - position);
        headBuf.rewind();
        if (headBuf.limit() != Packet.Head.HEADER_LEN) {
            headBuf.rewind();
            throw new IOException("decode head error;len=" + headBuf.limit());
        }
        // 解析包头
        byte version = headBuf.get();
        if (version != Packet.Head.VERSION) {
            headBuf.rewind();
            throw new IOException("decode head error;version=" + version);
        }
        long pkgLen = headBuf.getLong();
        byte type = headBuf.get();
        short event = headBuf.getShort();
        short status = headBuf.getShort();
        head.setBodyLen(pkgLen);
        head.setType(type);
        head.setEvent(event);
        head.setStatus(status);
        headBuf.rewind();
        // 如果包体长度为0则表示数据包解析完毕
        if (pkgLen == 0) {
            return Pair.build(true, packet);
        }
        // 重置包体缓存
        packet.reset();
        ByteBuffer bodyBuf = ByteBuffer.allocate((int) pkgLen);
        packet.setBody(bodyBuf);
        // 接收数据还不完整，先缓存下来
        if (bodyBuf.remaining() > buffer.length - readHeadLen) {
            bodyBuf.put(buffer, readHeadLen, buffer.length - readHeadLen);
            return FALSE;
        }
        bodyBuf.put(buffer, readHeadLen, buffer.length - readHeadLen);
        bodyBuf.flip();
        // 解析包体
        if (bodyBuf.limit() != pkgLen) {
            throw new IOException("decode body error;len=" + bodyBuf.limit());
        }
        // 解析包体数据
        byte[] body = new byte[bodyBuf.limit()];
        bodyBuf.get(body);
        // 解析包体数据
        if (head.getType() == PacketType.EVENT) {
            packet.setData(jsonSupport.readValue(session.getPath(), body, event));
        }
        return Pair.build(true, packet);
    }

    /**
     * 重置解码器，用于解决粘包问题
     */
    public void reset() {
    }
}
