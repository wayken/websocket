package cloud.apposs.websocket.protocol;

import java.io.OutputStream;

public class JsonSupportWrapper implements JsonSupport {
    private final JsonSupport delegate;

    public JsonSupportWrapper(JsonSupport delegate) {
        this.delegate = delegate;
    }

    @Override
    public void addEventMapping(String namespace, String command, Class<?> commandClass) {
        delegate.addEventMapping(namespace, command, commandClass);
    }

    @Override
    public Object readValue(String namespace, byte[] content, short event) throws Exception {
        return delegate.readValue(namespace, content, event);
    }

    @Override
    public void writeValue(OutputStream out, Object value) throws Exception {
        delegate.writeValue(out, value);
    }
}
