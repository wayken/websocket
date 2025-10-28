package cloud.apposs.websocket.sample.bean;

import java.util.List;

public class ChatUsers {
    private List<User> users;
    private String message;

    public ChatUsers() {
    }

    public ChatUsers(List<User> users, String message) {
        super();
        this.users = users;
        this.message = message;
    }

    public List<User> getUsers() {
        return users;
    }

    public void setUsers(List<User> users) {
        this.users = users;
    }

    public String getMessage() {
        return message;
    }
    public void setMessage(String message) {
        this.message = message;
    }

    public static class User {
        private String username;
        private String password;

        public User() {
        }

        public User(String username, String password) {
            super();
            this.username = username;
            this.password = password;
        }

        public String getUsername() {
            return username;
        }
        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }
        public void setPassword(String password) {
            this.password = password;
        }
    }
}
