package cloud.apposs.websocket.sample.bean;

public class ChatObject {
    private String username;
    private String message;

    public ChatObject() {
    }

    public ChatObject(String username, String message) {
        super();
        this.username = username;
        this.message = message;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getMessage() {
        return message;
    }
    public void setMessage(String message) {
        this.message = message;
    }
}
