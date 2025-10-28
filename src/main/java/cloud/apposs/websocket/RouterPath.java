package cloud.apposs.websocket;

public class RouterPath {
    /**
     * WebSocket Path
     */
    private final String path;

    /**
     * WebSocket 指令
     */
    private final String command;

    public RouterPath(String path, String command) {
        this.path = path;
        this.command = command;
    }

    public String getPath() {
        return path;
    }

    public String getCommand() {
        return command;
    }

    @Override
    public int hashCode() {
        return toString().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof RouterPath)) {
            return false;
        }
        RouterPath other = (RouterPath) obj;
        return path.equals(other.path) && command.equals(other.command);
    }

    @Override
    public String toString() {
        StringBuilder info = new StringBuilder(128);
        info.append("{");
        info.append("Path: ").append(path).append(", ");
        info.append("Command: ").append(command);
        info.append("}");
        return info.toString();
    }
}
