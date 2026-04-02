package cloud.apposs.websocket.protocol;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

public class JsonSupportWrapper implements JsonSupport {
    private final JsonSupport delegate;

    public JsonSupportWrapper(JsonSupport delegate) {
        this.delegate = delegate;
    }

    @Override
    public void addCommandMapping(String namespace, String command, List<Class<?>> commandClass) {
        delegate.addCommandMapping(namespace, command, commandClass);
    }

    @Override
    public <T> T readValue(Packet packet, InputStream content, Class<T> valueType) throws Exception {
        return delegate.readValue(packet, content, valueType);
    }

    @Override
    public void writeValue(OutputStream content, Object value) throws Exception {
        delegate.writeValue(content, value);
    }

    @Override
    public List<byte[]> getBuffers() {
        return delegate.getBuffers();
    }
}
