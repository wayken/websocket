package cloud.apposs.websocket.protocol;

import java.io.Serializable;
import java.nio.ByteBuffer;

/**
 * WebSocket 二进制数据包，数据包格式如下：
 * <pre>
 * +---------+--------------+------------+------------+--------+------+
 * | VERSION | PKG_LEN_SIZE | EVENT_TYPE | EVENT_NAME | STATUS | DATA |
 * +---------+--------------+------------+------------+--------+------+
 * | 1 byte  | 8 byte       | 1 byte     | 2 byte     | 2 byte | bytes|
 * +---------+--------------+------------+------------+--------+------+
 * </pre>
 * 其中包含头部数据和业务数据，
 * 头部数据格式如下：
 * <pre>
 *     VERSION: 协议版本号，目前为1
 *     PKG_LEN_SIZE: 数据包长度，占8个字节，最大支持8个字节的数据包长度
 *     EVENT_TYPE: 事件类型，详见{@link PacketType}
 *     EVENT_NAME: 事件名称，由业务自定义，最大支持65535个事件
 *     STATUS: 状态码，表示处理状态，取值范围为[0, 65535]
 * </pre>
 * 业务数据格式由业务自定义，任意长度任意数据类型，具体格式由用户自定义
 */
public class Packet implements Serializable {
    private static final long serialVersionUID = 1560259536486711426L;

    private final Head head = new Head();

    /**
     * 接收到的包体数据
     */
    private ByteBuffer body;

    /**
     * 解析后的数据
     */
    private Object data;

    public Packet() {
    }

    public Packet(PacketType type) {
        head.setType(type.getValue());
    }

    public Head getHead() {
        return head;
    }

    public ByteBuffer getBody() {
        return body;
    }

    public void setBody(ByteBuffer body) {
        this.body = body;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }

    public short getEvent() {
        return head.getEvent();
    }

    public long getBodyLen() {
        return head.getBodyLen();
    }

    public void setBodyLen(long bodyLen) {
        head.setBodyLen(bodyLen);
    }

    public PacketType getType() {
        return head.getType();
    }

    public void setType(PacketType type) {
        head.setType(type);
    }

    public void setEvent(short event) {
        head.setEvent(event);
    }

    public short getStatus() {
        return head.getStatus();
    }

    public void setStatus(short status) {
        head.setStatus(status);
    }

    public String getCommand() {
        return String.valueOf(head.getEvent());
    }

    public void reset() {
        if (body != null) {
            body.flip();
            body = null;
        }
    }

    public static class Head {
        /**
         * 包头大小，固定值
         */
        public static final int HEADER_LEN = 14;

        /**
         * 协议版本号，如果head的格式有变化，例如扩展成40个字节，此时解包的地方就可以根据version来做兼容处理
         */
        public static final byte VERSION = 0x1;

        /**
         * 接收到的包头数据
         */
        private ByteBuffer headBuf = ByteBuffer.allocate(HEADER_LEN);

        /**
         * 包体长度
         */
        private long bodyLen;

        /**
         * 事件类型
         */
        private PacketType type;

        /**
         * 事件名称，由业务自定义，最大支持65535个事件
         */
        private short event;

        /**
         * 状态码
         */
        private short status;

        public short getEvent() {
            return event;
        }

        public ByteBuffer getHeadBuf() {
            return headBuf;
        }

        public long getBodyLen() {
            return bodyLen;
        }

        public void setBodyLen(long bodyLen) {
            this.bodyLen = bodyLen;
        }

        public PacketType getType() {
            return type;
        }

        public void setType(byte type) {
            this.type = PacketType.valueOf(type);
        }

        public void setType(PacketType type) {
            this.type = type;
        }

        public void setEvent(short event) {
            this.event = event;
        }

        public short getStatus() {
            return status;
        }

        public void setStatus(short status) {
            this.status = status;
        }
    }
}
