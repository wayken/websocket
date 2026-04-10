package cloud.apposs.websocket.validator.checker;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 被注释的元素必须是一个{@link java.util.UUID}类型的字符串，并且必须满足UUID的格式要求。
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Uuid {
    /**
     * 是否强制需要此值
     */
    boolean require() default true;

    /**
     * 错误消息输出
     */
    String message() default "";
}
