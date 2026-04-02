package cloud.apposs.websocket.protocol;

import cloud.apposs.util.Pair;
import cloud.apposs.websocket.WSSession;
import cloud.apposs.websocket.util.Buffers;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Base64;
import java.util.LinkedList;

/**
 * WS二进制数据包解码器，每个 Channel 对应一个实例，数据包格式详见{@link Packet}，
 * 说明：WebSocket 协议保证每帧是完整消息，因此主包头部和 DATA 在同一帧内完整到达
 */
public class PacketDecoder {
    private static final String QUOTES = "\"";

    private final JsonSupport jsonSupport;

    private final String charset;

    // 上一次发送的二进制数据包，用于处理附件多次分片发送的情况
    private Packet lastBinaryPacket;

    public static final Pair<Boolean, Packet> INCOMPLETE = Pair.build(false, null);

    public PacketDecoder(JsonSupport jsonSupport, String charset) {
        this.jsonSupport = jsonSupport;
        this.charset = charset;
    }

    /**
     * 解码数据包，数据包格式详见{@link Packet}
     *
     * @param  session 会话
     * @param  frame   本帧完整字节数据
     * @return 解码完整的 Packet（含所有附件）
     *  1. 如果返回null则表示解码失败
     *  2. 如果返回Pair.key为false表示数据包还没解包完整，需要等待下一次数据包（如还在等待附件帧）
     *  3. 如果返回Pair.key为true表示数据包解码完整，Pair.value为解码后的数据包
     */
    public Pair<Boolean, Packet> decode(WSSession session, ByteBuffer frame) throws Exception {
        if (frame == null || frame.remaining() == 0) {
            return null;
        }

        // 当前有等待附件的主包，本帧是附件帧
        if (lastBinaryPacket != null && !lastBinaryPacket.isAttachmentsLoaded()) {
            return parseBinary(session, frame, lastBinaryPacket);
        }

        // 解析主据包头，主包头部和 DATA 在同一帧内完整到达
        if (frame.remaining() < Packet.HEADER_LEN) {
            throw new IOException("Frame too short: " + frame.remaining() + ", Expected at least " + Packet.HEADER_LEN + " bytes");
        }
        byte version = frame.get();
        if (version != Packet.VERSION) {
            throw new IOException("Unsupported protocol version: " + version);
        }
        byte type = frame.get();
        short status = frame.getShort();
        Packet packet = new Packet(PacketType.valueOf(type));
        packet.setVersion(version);
        packet.setStatus(status);
        packet.setNamespace(session.getNamespace().getName());
        // 解析元数据
        parseMetadata(session, frame, packet);
        // 解析参数数据
        parseParameter(session, frame, packet);
        // 如果有附件还未全部到达，返回等待状态
        if (!packet.isAttachmentsLoaded()) {
            return INCOMPLETE;
        }
        return Pair.build(true, packet);
    }

    private boolean parseMetadata(WSSession session, ByteBuffer frame, Packet packet) throws Exception {
        if (packet.getType() != PacketType.COMMAND) {
            return false;
        }
        // 查找元数据结束位置（以 '#' 字符分隔）
        int separatorOffset = Buffers.bytesBefore(frame, (byte) Packet.SEPARATOR);
        if (separatorOffset < 0) {
            throw new IOException("Missing metadata separator '#' in packet");
        }
        // 读取数据协议中元数据字节
        byte[] metaBytes = new byte[separatorOffset];
        frame.get(metaBytes);
        // 跳过数据协议包中 '#' 分隔符
        frame.get();

        Metadata metadata = jsonSupport.readValue(packet, Buffers.wrappedInputStream(metaBytes), Metadata.class);
        packet.setMetadata(metadata);
        return true;
    }

    private boolean parseParameter(WSSession session, ByteBuffer frame, Packet packet) throws Exception {
        if (packet.getType() != PacketType.COMMAND) {
            return false;
        }
        if (!frame.hasRemaining()) {
            return false;
        }
        // 如果有附件，保存主包等待后续附件帧
        if (packet.getMetadata().hasAttachment() && !packet.isAttachmentsLoaded()) {
            packet.setDataSource(Buffers.copiedBuffer(frame));
            lastBinaryPacket = packet;
            return false;
        }
        // 读取剩余参数数据
        byte[] paramBytes = new byte[frame.remaining()];
        frame.get(paramBytes);

        Parameter parameter = jsonSupport.readValue(packet, Buffers.wrappedInputStream(paramBytes), Parameter.class);
        packet.setParameter(parameter);
        return true;
    }

    private Pair<Boolean, Packet> parseBinary(WSSession session, ByteBuffer frame, Packet packet) throws Exception {
        packet.addAttachment(Base64.getEncoder().encode(frame));
        // 附件还没到达指定数量，返回等待状态等待下一次附件帧
        if (!packet.isAttachmentsLoaded()) {
            return INCOMPLETE;
        }
        // 解码数据包，将所有点位符替换为附件数据再进行参数解析
        LinkedList<ByteBuffer> slices = new LinkedList<>();
        ByteBuffer source = packet.getDataSource();
        for (int i = 0; i < packet.getAttachments().size(); i++) {
            ByteBuffer attachment = packet.getAttachments().get(i);
            String placeholder = String.format("{\"%s\":true,\"%s\":%d}", Packet.ATTACHMENT_PLACEHOLDER, Packet.ATTACHMENT_INDEX, i);
            ByteBuffer scanValue = Buffers.wrappedBuffer(placeholder, charset);
            int position = Buffers.findBufferIndex(source, scanValue);
            if (position == -1) {
                throw new IllegalStateException("Can't find attachment by index: " + i + " in packet source");
            }
            ByteBuffer prefixBuf = Buffers.sliceBuffer(source, source.position(), position - source.position());
            slices.add(prefixBuf);
            slices.add(Buffers.wrappedBuffer(QUOTES, charset));
            slices.add(attachment);
            slices.add(Buffers.wrappedBuffer(QUOTES, charset));
            source.position(position + scanValue.remaining());
        }
        slices.add(Buffers.sliceBuffer(source, source.position(), source.remaining()));
        ByteBuffer compositeBuffer = Buffers.wrappedBuffer(slices.toArray(new ByteBuffer[0]));
        parseParameter(session, compositeBuffer, packet);
        lastBinaryPacket = null;
        return Pair.build(true, packet);
    }
}
