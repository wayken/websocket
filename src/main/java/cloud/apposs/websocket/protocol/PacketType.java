package cloud.apposs.websocket.protocol;

/**
 * WebSocket 二进制数据包事件类型
 */
public enum PacketType {
    HANDSHAKE((byte) 0), EVENT((byte) 1), DISCONNECT((byte) 2), ERROR((byte) 3);

    public static final PacketType[] VALUES = values();
    private final byte value;

    PacketType(byte value) {
        this.value = value;
    }

    public byte getValue() {
        return value;
    }

    public static PacketType valueOf(byte value) {
        for (PacketType type : VALUES) {
            if (type.getValue() == value) {
                return type;
            }
        }
        throw new IllegalStateException();
    }
}
