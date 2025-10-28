package cloud.apposs.websocket.protocol;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

public class JacksonJsonSupport implements JsonSupport {
    final Map<Short, Class<?>> eventMapping;

    protected final ObjectMapper objectMapper;

    public JacksonJsonSupport() {
        this.eventMapping = new HashMap<Short, Class<?>>();
        this.objectMapper = new ObjectMapper();
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
    }

    @Override
    public void addEventMapping(String namespace, String command, Class<?> commandClass) {
        short event = Short.parseShort(command);
        eventMapping.put(event, commandClass);
    }

    @Override
    public Object readValue(String namespace, byte[] content, short event) throws Exception {
        Class<?> commandClass = eventMapping.get(event);
        if (commandClass == null) {
            return null;
        }
        // 如果是字节数组则直接返回
        if (commandClass == byte[].class) {
            return content;
        }
        return objectMapper.readValue(content, commandClass);
    }

    @Override
    public void writeValue(OutputStream out, Object value) throws Exception {
        objectMapper.writeValue(out, value);
    }
}
