package cloud.apposs.websocket.protocol;

import cloud.apposs.websocket.util.Buffers;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.List;

/**
 * WebSocket数据包编码器，数据包格式详见{@link Packet}
 */
public class PacketEncoder {
    /**
     * 数据包编码
     *
     * @param  packet 数据包
     * @return 编码后的字节流
     */
    public static byte[] encode(Packet packet, JsonSupport jsonSupport) throws Exception {
        if (packet == null) {
            return null;
        }

        // 如果包体数据不为空，编码数据包体，同时获取数据包体的字节长度
        byte[] data = null;
        switch (packet.getType()) {
            case HANDSHAKE: {
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                jsonSupport.writeValue(outputStream, packet.getParameter());
                data = outputStream.toByteArray();
                break;
            }
            case COMMAND: {
                // 编码参数数据
                ByteArrayOutputStream paramStream = new ByteArrayOutputStream();
                jsonSupport.writeValue(paramStream, packet.getParameter().getArguments());
                byte[] paramBytes = paramStream.toByteArray();
                // 附件数据序列化
                List<byte[]> buffers = jsonSupport.getBuffers();
                if (!buffers.isEmpty()) {
                    packet.getMetadata().setAttachmentNum(buffers.size());
                    for (byte[] array : buffers) {
                        packet.addAttachment(Buffers.wrappedBuffer(array));
                    }
                }
                // 编码元数据
                ByteArrayOutputStream metaStream = new ByteArrayOutputStream();
                jsonSupport.writeValue(metaStream, packet.getMetadata());
                byte[] metaBytes = metaStream.toByteArray();
                // 拼接：METADATA + '#' + PARAMETER
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                outputStream.write(metaBytes);
                outputStream.write(Packet.SEPARATOR);
                outputStream.write(paramBytes);
                data = outputStream.toByteArray();
                break;
            }
            default: {
                data = new byte[0];
                break;
            }
        }
        // 头部：VERSION(1) + EVENT_TYPE(1) + STATUS(2)
        ByteBuffer buffer = ByteBuffer.allocate(Packet.HEADER_LEN + data.length);
        buffer.put(Packet.VERSION);
        buffer.put(packet.getType().getValue());
        buffer.putShort(packet.getStatus());
        buffer.put(data);
        return buffer.array();
    }
}
