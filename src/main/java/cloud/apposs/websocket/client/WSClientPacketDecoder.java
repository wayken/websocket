package cloud.apposs.websocket.client;

import cloud.apposs.websocket.protocol.*;
import cloud.apposs.websocket.util.Buffers;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Base64;
import java.util.LinkedList;

/**
 * 客户端数据包解码器，支持附件多帧接收和占位符替换
 */
public class WSClientPacketDecoder {
    private static final String QUOTES = "\"";

    private final JsonSupport jsonSupport;
    private final String charset;

    // 上一次解码的主包，用于等待附件帧
    private Packet lastBinaryPacket;

    public WSClientPacketDecoder(JsonSupport jsonSupport, String charset) {
        this.jsonSupport = jsonSupport;
        this.charset = charset;
    }

    /**
     * 解码服务端发来的二进制帧
     *
     * @return 解码完成的Packet；如果还在等待附件帧返回null
     */
    public Packet decode(ByteBuffer frame) throws Exception {
        if (frame == null || frame.remaining() == 0) {
            return null;
        }

        // 当前有等待附件的主包，本帧是附件帧
        if (lastBinaryPacket != null && !lastBinaryPacket.isAttachmentsLoaded()) {
            return parseBinary(frame, lastBinaryPacket);
        }

        if (frame.remaining() < Packet.HEADER_LEN) {
            throw new IOException("Frame too short: " + frame.remaining());
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

        if (packet.getType() == PacketType.COMMAND) {
            int separatorOffset = Buffers.bytesBefore(frame, (byte) Packet.SEPARATOR);
            if (separatorOffset < 0) {
                throw new IOException("Missing metadata separator '#' in packet");
            }
            byte[] metaBytes = new byte[separatorOffset];
            frame.get(metaBytes);
            frame.get(); // skip '#'
            Metadata metadata = jsonSupport.readValue(packet, Buffers.wrappedInputStream(metaBytes), Metadata.class);
            packet.setMetadata(metadata);

            // 有附件时保存主包等待后续附件帧
            if (metadata.hasAttachment()) {
                if (frame.hasRemaining()) {
                    packet.setDataSource(Buffers.copiedBuffer(frame));
                }
                lastBinaryPacket = packet;
                return null;
            }
            // 无附件直接解析参数
            if (frame.hasRemaining()) {
                byte[] paramBytes = new byte[frame.remaining()];
                frame.get(paramBytes);
                Parameter parameter = jsonSupport.readValue(packet, Buffers.wrappedInputStream(paramBytes), Parameter.class);
                packet.setParameter(parameter);
            }
        } else if (packet.getType() == PacketType.HANDSHAKE) {
            if (frame.hasRemaining()) {
                byte[] paramBytes = new byte[frame.remaining()];
                frame.get(paramBytes);
                Parameter parameter = jsonSupport.readValue(packet, Buffers.wrappedInputStream(paramBytes), Parameter.class);
                packet.setParameter(parameter);
            }
        }
        return packet;
    }

    private Packet parseBinary(ByteBuffer frame, Packet packet) throws Exception {
        packet.addAttachment(Base64.getEncoder().encode(frame));
        if (!packet.isAttachmentsLoaded()) {
            return null;
        }
        // 所有附件到齐，替换占位符后解析参数
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
        byte[] paramBytes = new byte[compositeBuffer.remaining()];
        compositeBuffer.get(paramBytes);
        Parameter parameter = jsonSupport.readValue(packet, Buffers.wrappedInputStream(paramBytes), Parameter.class);
        packet.setParameter(parameter);
        lastBinaryPacket = null;
        return packet;
    }
}
