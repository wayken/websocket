package cloud.apposs.websocket.protocol;

/**
 * 数据包参数列表，详见{@link Packet}
 */
public class Parameter {
    private Object arguments;

    public void setArguments(Object arguments) {
        this.arguments = arguments;
    }

    public <T> T getArguments() {
        return (T) arguments;
    }
}
