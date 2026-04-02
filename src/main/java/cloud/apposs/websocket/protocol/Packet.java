package cloud.apposs.websocket.protocol;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * WebSocket 二进制数据包，数据包格式如下：
 * <pre>
 * +---------+--------------+--------+----------+------+-----------+
 * | VERSION | COMMAND_TYPE | STATUS | METADATA |  -   | PARAMETER |
 * +---------+--------------+--------+----------+------+-----------+
 * | 8 bit   | 8 bit        | 16 bit | bits     | char | bits      |
 * +---------+--------------+--------+----------+------+-----------+
 * 如果后续有附件数据包，则在DATA部分后面会发送一个或多个附件数据包，附件数据包格式如下：
 * +-------------+
 * | Binary Data |
 * +-------------+
 * 头部 HEADER 数据格式（4个字节）如下：
 * <pre>
 *     VERSION: 协议版本号，目前为1
 *     COMMAND_TYPE: 指令类型，详见{@link PacketType}
 *     STATUS: 状态码，表示处理状态，取值范围为[0, 65535]
 * </pre>
 * 当事件类型为 {@link PacketType#COMMAND}时会扩展出 METADATA 和 PARAMETER 两个字段，分别表示指令的元数据和参数数据，其中
 * 元数据 METADATA 数据格式如下：
 * <pre>
 *     METADATA: JSON对象字符串，包含系统预定义的指令参数和用户自定义的业务参数，数据格式为{"_cmd": "xxx", "_id":xxx, "_num":xxx,...}
 *     其中：
 *     1. _cmd表示指令名称（字符串），示例："CommandChat"
 *     2. _id表示当次请求指令ID（字符串），主要服务于RPC通讯，可选项，示例："123e4567-e89b-12d3-a456-426614174000"
 *     3. _num表示有多少附件发送（整数），示例：2，当有附件时后续会发送_num个附件数据包
 * </pre>
 * 参数数据（PARAMETER）数据格式如下：
 * <pre>
 *     PARAMETER: 数组格式的参数数据，数组内的数据格式由用户自定义，其中
 *     1. 参数格式为数据列表，示例：["Hello, World!, {"key": "value"}, 123]
 *     2. 参数数据类型由用户自定义，示例：字符串、JSON对象、整数等自由定义
 * </pre>
 * NOTE：在元数据和参数数据中间是以"-"字符分隔的，主要是为了方便系统解析
 * 关于附件数据说明：
 * <pre>
 *     1. 当业务数据有附带附件时，其中的数据格式需要有点位符，示例：{"message": "Hello, World!", "file": {"_placeholder":true,"_num":0}}
 *     2. 其中_attachment_index表示附件数据包的索引，索引从0开始递增
 * </pre>
 * 业务数据格式由业务自定义，任意长度任意数据类型，具体格式由用户自定义，
 * 完整的数据包示例：
 * <pre>
 * 1000{"_cmd": "xxx", "_id": "xxx", "_num": 2}-[{"message": "Hello, World!", "file": {"_placeholder":true,"_num":0}}, "xxx", 123]
 * </pre>
 */
public class Packet implements Serializable {
    private static final long serialVersionUID = 1560259536486711426L;

    public static final String ATTACHMENT_PLACEHOLDER = "_placeholder";
    public static final String ATTACHMENT_INDEX = "_num";

    // 包头固定长度：VERSION(1) + COMMAND_TYPE(1) + STATUS(2) = 4 字节
    public static final int HEADER_LEN = 4;
    // 协议版本号，如果数据格式有变化，例如扩展成10个字节，此时解包的地方就可以根据version来做兼容处理
    public static final byte VERSION = 0x1;
    // 数据部分分隔符，主要用于分隔METADATA和PARAMETER两个部分，方便系统解析
    public static final byte SEPARATOR = (byte) '#';

    // 协议版本
    private byte version = VERSION;

    // 指令类型
    private PacketType type;

    // 状态码 [0, 65535]，用 int 存储避免符号位问题
    private short status;

    // 命名空间
    private String namespace;

    // 指令元数据
    private Metadata metadata = new Metadata();

    // 业务参数列表
    private Parameter parameter = new Parameter();

    // 附件二进制数据列表，顺序与 {@link Metadata#attachmentNum} 索引对应
    private List<ByteBuffer> attachments;

    // 原始包体字节（解码中间态）
    private transient ByteBuffer dataSource;

    public Packet() {
    }

    public Packet(PacketType type) {
        this.type = type;
    }

    public byte getVersion() {
        return version;
    }

    public void setVersion(byte version) {
        this.version = version;
    }

    public PacketType getType() {
        return type;
    }

    public void setType(PacketType type) {
        this.type = type;
    }

    public short getStatus() {
        return status;
    }

    public void setStatus(short status) {
        this.status = status;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public String getCommand() {
        return metadata.getCommandName();
    }

    public void setCommand(String command) {
        metadata.setCommandName(command);
    }

    public Metadata getMetadata() {
        return metadata;
    }

    public void setMetadata(Metadata metadata) {
        this.metadata = metadata;
    }

    public Parameter getParameter() {
        return parameter;
    }

    public void setParameter(Parameter parameter) {
        this.parameter = parameter;
    }

    public List<ByteBuffer> getAttachments() {
        return attachments;
    }

    public void setAttachments(List<ByteBuffer> attachments) {
        this.attachments = attachments;
    }

    public void addAttachment(ByteBuffer attachment) {
        if (this.attachments == null) {
            this.attachments = new ArrayList<>(metadata.getAttachmentNum());
        }
        this.attachments.add(attachment);
    }

    public ByteBuffer getDataSource() {
        return dataSource;
    }

    public void setDataSource(ByteBuffer dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * 检查是否所有附件数据包都已加载完成
     *
     * @return true 如果没有附件或所有附件都已加载完成，false 如果还有未加载的附件
     */
    public boolean isAttachmentsLoaded() {
        int received = attachments == null ? 0 : attachments.size();
        int attachmentNum = metadata.getAttachmentNum();
        return received >= attachmentNum;
    }

    @Override
    public String toString() {
        return "Packet{type=" + type
                + ", status=" + status
                + ", metadata=" + metadata
                + "}";
    }
}
