package cloud.apposs.websocket.protocol;

public class Attachment {
    /** 附件索引，从 0 开始，对应 attachments 列表中的位置 */
    private int num;

    public int getNum() {
        return num;
    }

    @Override
    public String toString() {
        return "Attachment{num=" + num + "}";
    }
}
