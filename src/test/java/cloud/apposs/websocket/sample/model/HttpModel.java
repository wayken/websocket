package cloud.apposs.websocket.sample.model;

import cloud.apposs.rest.validator.checker.NotBlank;

public class HttpModel {
    public static class User {
        @NotBlank
        private String name;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }
}
