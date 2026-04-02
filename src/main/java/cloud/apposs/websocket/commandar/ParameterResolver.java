package cloud.apposs.websocket.commandar;

import cloud.apposs.websocket.WSSession;
import cloud.apposs.websocket.protocol.Metadata;
import cloud.apposs.websocket.protocol.Packet;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * 方法参数解析器，用于解析数据包的参数数据，映射到指令方法的参数列表中
 */
public final class ParameterResolver {
    /**
     * 解析方法参数，操作流程如下：
     * <pre>
     * 1. 如果是系统参数（WSSession、Metadata）则直接从当前会话或数据包中获取并初始化
     * 2. 如果是用户参数，则从数据包的参数列表中获取并初始化
     * </pre>
     *
     * @param commandar 当前方法
     * @param session   当前会话
     * @param packet    当前会话解析数据包
     */
    public static Object[] resolveParameterArguments(Commandar commandar, WSSession session, Packet packet) {
        Method method = commandar.getMethod();
        Class<?>[] parameterTypes = method.getParameterTypes();
        Object[] arguments = new Object[parameterTypes.length];
        int sessionIndex = getParameterIndex(method, WSSession.class);
        if (sessionIndex != -1) {
            arguments[sessionIndex] = session;
        }
        int metadataIndex = getParameterIndex(method, Metadata.class);
        if (metadataIndex != -1) {
            arguments[metadataIndex] = packet.getMetadata();
        }
        List<Object> parameters = packet.getParameter().getArguments();
        List<Integer> dataIndexes = getParameterIndexes(method);
        int i = 0;
        for (int index : dataIndexes) {
            if (parameters.size() <= i) {
                arguments[index] = null;
            } else {
                arguments[index] = parameters.get(i);
            }
            i++;
        }
        return arguments;
    }

    /**
     * 解析方法 JSON Object 参数类型
     *
     * @param  commandar 当前指令方法
     * @return 返回解析后的参数类型
     */
    public static List<Class<?>> resolveParameterTypes(Commandar commandar) {
        Method method = commandar.getMethod();
        Class<?>[] parameters = method.getParameterTypes();
        List<Class<?>> types = new ArrayList<Class<?>>();
        for (Class<?> type : parameters) {
            if (isSystemParameter(type)) {
                continue;
            }
            types.add(type);
        }
        return types;
    }

    private static int getParameterIndex(Method method, Class<?> clazz) {
        int index = 0;
        for (Class<?> type : method.getParameterTypes()) {
            if (type.equals(clazz)) {
                return index;
            }
            index++;
        }
        return -1;
    }

    private static List<Integer> getParameterIndexes(Method method) {
        List<Integer> parameters = new ArrayList<Integer>();
        int index = 0;
        for (Class<?> type : method.getParameterTypes()) {
            if (!isSystemParameter(type)) {
                parameters.add(index);
            }
            index++;
        }
        return parameters;
    }

    private static boolean isSystemParameter(Class<?> clazz) {
        return clazz.equals(WSSession.class) || clazz.equals(Metadata.class);
    }
}
