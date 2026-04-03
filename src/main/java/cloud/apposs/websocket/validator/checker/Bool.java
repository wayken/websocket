package cloud.apposs.websocket.validator.checker;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 被注释的元素必须Boolean
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Bool {
    /**
     * 是否强制需要此值
     */
    boolean require() default true;

    /**
     * 默认值
     */
    boolean value() default false;

    /**
     * 错误消息输出
     */
    String message() default "";
}
