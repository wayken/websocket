package cloud.apposs.websocket.protocol;

/**
 * 数据包元数据，详见{@link Packet}
 */
public class Metadata {
    public static final String METADATA_COMMAND_NAME = "_cmd";
    public static final String METADATA_COMMAND_ID = "_id";
    public static final String METADATA_ATTACHMENTS = "_num";

    // 指令名称
    private String commandName;

    // 请求指令ID
    private String commandId;

    // 附件数量
    private int attachmentNum = 0;

    public String getCommandName() {
        return commandName;
    }

    public void setCommandName(String commandName) {
        this.commandName = commandName;
    }

    public String getCommandId() {
        return commandId;
    }

    public void setCommandId(String commandId) {
        this.commandId = commandId;
    }

    public int getAttachmentNum() {
        return attachmentNum;
    }

    public void setAttachmentNum(int attachmentNum) {
        this.attachmentNum = attachmentNum;
    }

    public boolean hasCommandId() {
        return commandId != null && !commandId.isEmpty();
    }

    public boolean hasAttachment() {
        return attachmentNum > 0;
    }

    @Override
    public String toString() {
        return "Metadata{" +
                "command='" + commandName + '\'' +
                ", id='" + commandId + '\'' +
                ", attachment=" + attachmentNum +
                '}';
    }
}
