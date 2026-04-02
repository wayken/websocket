package cloud.apposs.websocket.protocol;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonArrayFormatVisitor;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonFormatTypes;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonFormatVisitorWrapper;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.ser.BeanSerializerModifier;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.fasterxml.jackson.databind.type.ArrayType;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Type;
import java.util.*;

/**
 * 基于 Jackson 的 JsonSupport 实现
 */
public class JacksonJsonSupport implements JsonSupport {
    private final ObjectMapper objectMapper;

    private final Map<CommandKey, List<Class<?>>> commandMapping;

    private final ThreadLocal<Packet> packetThreadLocal = new ThreadLocal<Packet>();

    protected final ExBeanSerializerModifier modifier = new ExBeanSerializerModifier();

    public JacksonJsonSupport() {
        this.objectMapper = new ObjectMapper();
        this.commandMapping = new HashMap<>();
        handleMapperInit(objectMapper);
    }

    @Override
    public void addCommandMapping(String namespace, String command, List<Class<?>> commandClass) {
        CommandKey commandKey = new CommandKey(namespace, command);
        List<Class<?>> commanders = commandMapping.computeIfAbsent(commandKey, k -> new ArrayList<>());
        commanders.addAll(commandClass);
    }

    @Override
    public <T> T readValue(Packet packet, InputStream content, Class<T> valueType) throws Exception {
        packetThreadLocal.set(packet);
        return objectMapper.readValue(content, valueType);
    }

    @Override
    public void writeValue(OutputStream content, Object value) throws Exception {
        modifier.getSerializer().clear();
        objectMapper.writeValue(content, value);
    }

    @Override
    public List<byte[]> getBuffers() {
        return modifier.getSerializer().getBuffers();
    }

    private void handleMapperInit(ObjectMapper objectMapper) {
        // 配置序列化和反序列化选项
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        // 注册相关的自定义序列化、反序列化器
        SimpleModule module = new SimpleModule();
        module.setSerializerModifier(modifier);
        module.addSerializer(Metadata.class, new MetadataSerializer());
        module.addDeserializer(Metadata.class, new MetadataDeserializer());
        module.addDeserializer(Parameter.class, new ParameterDeserializer());
        objectMapper.registerModule(module);
    }

    public static class CommandKey {
        private final String namespace;
        private final String command;

        public CommandKey(String namespace, String command) {
            this.namespace = namespace;
            this.command = command;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((command == null) ? 0 : command.hashCode());
            result = prime * result + ((namespace == null) ? 0 : namespace.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            CommandKey other = (CommandKey) obj;
            if (command == null) {
                if (other.command != null) {
                    return false;
                }
            } else if (!command.equals(other.command)) {
                return false;
            }
            if (namespace == null) {
                if (other.namespace != null) {
                    return false;
                }
            } else if (!namespace.equals(other.namespace)) {
                return false;
            }
            return true;
        }

        @Override
        public String toString() {
            return "CommandKey [namespace=" + namespace + ", command=" + command + "]";
        }
    }

    /**
     * {@link Metadata} 自定义序列化器，将 Metadata 字段序列化为 _cmd/_id/_num
     */
    private static class MetadataSerializer extends StdSerializer<Metadata> {
        public MetadataSerializer() {
            super(Metadata.class);
        }

        @Override
        public void serialize(Metadata metadata, JsonGenerator gen, SerializerProvider provider) throws IOException {
            gen.writeStartObject();
            if (metadata.getCommandName() != null) {
                gen.writeStringField(Metadata.METADATA_COMMAND_NAME, metadata.getCommandName());
            }
            if (metadata.hasCommandId()) {
                gen.writeStringField(Metadata.METADATA_COMMAND_ID, metadata.getCommandId());
            }
            if (metadata.hasAttachment()) {
                gen.writeNumberField(Metadata.METADATA_ATTACHMENTS, metadata.getAttachmentNum());
            }
            gen.writeEndObject();
        }
    }

    /**
     * {@link Metadata} 自定义反序列化器，将数据包中的 _cmd/_id/_num 映射到 Metadata 字段
     */
    private class MetadataDeserializer extends StdDeserializer<Metadata> {
        public MetadataDeserializer() {
            super(Metadata.class);
        }

        @Override
        public Metadata deserialize(JsonParser parser, DeserializationContext ctx) throws IOException {
            JsonNode node = parser.getCodec().readTree(parser);
            Metadata metadata = new Metadata();
            if (node.has(Metadata.METADATA_COMMAND_NAME)) {
                metadata.setCommandName(node.get(Metadata.METADATA_COMMAND_NAME).asText());
            }
            if (node.has(Metadata.METADATA_COMMAND_ID)) {
                metadata.setCommandId(node.get(Metadata.METADATA_COMMAND_ID).asText());
            }
            if (node.has(Metadata.METADATA_ATTACHMENTS)) {
                metadata.setAttachmentNum(node.get(Metadata.METADATA_ATTACHMENTS).asInt());
            }
            return metadata;
        }
    }

    /**
     * {@link Parameter} 自定义反序列化器，负责根据指令映射器上的参数类型信息，将数据包中的参数列表反序列化为对应的对象列表
     */
    private class ParameterDeserializer extends StdDeserializer<Parameter> {
        public ParameterDeserializer() {
            super(Parameter.class);
        }

        @Override
        public Parameter deserialize(JsonParser parser, DeserializationContext ctx) throws IOException {
            // 从 ThreadLocal 取当前数据包，查找对应的参数类型映射
            Packet packet = packetThreadLocal.get();
            if (packet == null) {
                throw new IOException("No packet context available for deserializing parameters");
            }
            CommandKey commandKey = new CommandKey(packet.getNamespace(), packet.getCommand());
            List<Class<?>> paramClasses = commandMapping.getOrDefault(commandKey, Collections.emptyList());

            JsonNode rootNode = parser.getCodec().readTree(parser);
            int dataSize = rootNode.size();
            List<Object> arguments = new ArrayList<>(dataSize);
            for (int i = 0; i < dataSize; i++) {
                JsonNode node = rootNode.get(i);
                if (i < paramClasses.size()) {
                    // 按注册的目标类型反序列化
                    try {
                        arguments.add(parser.getCodec().treeToValue(node, paramClasses.get(i)));
                    } catch (Exception e) {
                        String message = String.format("Failed to deserialize parameter at index %d to type %s for %s", i, paramClasses.get(i).getName(), commandKey);
                        throw new IOException(message, e);
                    }
                } else {
                    // 超出注册类型范围，保留为原始 JsonNode，不丢数据
                    arguments.add(node);
                }
            }

            Parameter parameter = new Parameter();
            parameter.setArguments(arguments);
            return parameter;
        }
    }

    public static class ByteArraySerializer extends StdSerializer<byte[]> {
        private static final long serialVersionUID = 1420082888596468148L;

        private final ThreadLocal<List<byte[]>> buffers = ThreadLocal.withInitial(ArrayList::new);

        public ByteArraySerializer() {
            super(byte[].class);
        }

        @Override
        public boolean isEmpty(byte[] value) {
            return (value == null) || (value.length == 0);
        }

        @Override
        public void serialize(byte[] value, JsonGenerator jgen, SerializerProvider provider) throws IOException {
            Map<String, Object> map = new HashMap<>();
            map.put(Packet.ATTACHMENT_PLACEHOLDER, true);
            map.put(Packet.ATTACHMENT_INDEX, buffers.get().size());
            jgen.writeObject(map);
            buffers.get().add(value);
        }

        @Override
        public void serializeWithType(byte[] value, JsonGenerator jgen, SerializerProvider provider, TypeSerializer typeSer)
                throws IOException, JsonGenerationException {
            serialize(value, jgen, provider);
        }

        @Override
        public JsonNode getSchema(SerializerProvider provider, Type typeHint) {
            ObjectNode o = createSchemaNode("array", true);
            ObjectNode itemSchema = createSchemaNode("string"); //binary values written as strings?
            return o.set("items", itemSchema);
        }

        @Override
        public void acceptJsonFormatVisitor(JsonFormatVisitorWrapper visitor, JavaType typeHint) throws JsonMappingException {
            if (visitor != null) {
                JsonArrayFormatVisitor v2 = visitor.expectArrayFormat(typeHint);
                if (v2 != null) {
                    v2.itemsFormat(JsonFormatTypes.STRING);
                }
            }
        }

        public List<byte[]> getBuffers() {
            return buffers.get();
        }

        public void clear() {
            buffers.set(new ArrayList<>());
        }
    }

    protected static class ExBeanSerializerModifier extends BeanSerializerModifier {
        private final ByteArraySerializer serializer = new ByteArraySerializer();

        @Override
        public JsonSerializer<?> modifyArraySerializer(SerializationConfig config, ArrayType valueType, BeanDescription beanDesc, JsonSerializer<?> serializer) {
            if (valueType.getRawClass().equals(byte[].class)) {
                return this.serializer;
            }
            return super.modifyArraySerializer(config, valueType, beanDesc, serializer);
        }

        public ByteArraySerializer getSerializer() {
            return serializer;
        }
    }
}
