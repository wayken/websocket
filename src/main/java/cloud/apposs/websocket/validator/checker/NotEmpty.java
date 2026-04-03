package cloud.apposs.websocket.validator.checker;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 被注释的对象必须非空，即
 * 1. 字符串的必须非空，即长度必须大于0
 * 2. List数组元素必须非空
 * 3. Map集合必须非空
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface NotEmpty {
    /**
     * 是否强制需要此值
     */
    boolean require() default true;

    /**
     * 错误消息输出
     */
    String message() default "";
}
