package cloud.apposs.websocket.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * WebSocket 事件发送注解，方法会进行相应的WebSocket指令匹配和回调，支持多个方法同时注解，可以通过{@link Order}调整执行顺序，
 * 被注解的方法参数可以由业务自定义数据类型，注意<b>自定义数据类型必须有默认构造函数</b>，代码示例如下
 * <pre>
 * {@code @OnEvent(Command.CHAT_EVENT)}
 * public void onEvent(WSSession session, String data) {
 *     System.out.println("event data: " + data);
 * }
 * {@code @OnEvent(Command.CHAT_EVENT2)}
 * public void onEvent(WSSession session, ChatObject data1, String data2) {
 *     System.out.println("event data: " + data1 + "-" + data2);
 * }
 * </pre>
 * 客户端代码示例如下
 * <pre>
 * socket.emit(0, "hello");
 * socket.emit(1, {name: "hello"}, "world");
 * </prev>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface OnEvent {
    /**
     * WebSocket 指令，
     * 因为采用二进制协议来传输 WebSocket 数据，所以指令为 short 类型，
     * 在一个WebSocket Path下的指令数量不能超过 65535 个
     */
    short value();
}
