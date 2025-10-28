package cloud.apposs.websocket.protocol;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;

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
        if (packet.getData() != null) {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            jsonSupport.writeValue(outputStream, packet.getData());
            data = outputStream.toByteArray();
        } else {
            data = new byte[0];
        }
        ByteBuffer buffer = ByteBuffer.allocate(Packet.Head.HEADER_LEN + data.length);
        // 编码数据包头
        buffer.put(Packet.Head.VERSION);
        buffer.putLong(data.length);
        buffer.put(packet.getType().getValue());
        buffer.putShort(packet.getEvent());
        buffer.putShort(packet.getStatus());
        buffer.put(data, 0, data.length);
        buffer.rewind();
        int len = buffer.limit() - buffer.position();
        byte[] response = new byte[len];
        buffer.get(response);
        return response;
    }
}
