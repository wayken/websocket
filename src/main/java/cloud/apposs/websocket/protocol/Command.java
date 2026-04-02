package cloud.apposs.websocket.protocol;

import java.util.List;

/**
 * WebSocket指令原始数据对象，包含命令名称和参数列表
 */
public class Command {
    private String name;

    private List<Object> arguments;

    public Command() {
    }

    public Command(String name, List<Object> arguments) {
        super();
        this.name = name;
        this.arguments = arguments;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Object> getArguments() {
        return arguments;
    }

    public void setArguments(List<Object> arguments) {
        this.arguments = arguments;
    }
}
