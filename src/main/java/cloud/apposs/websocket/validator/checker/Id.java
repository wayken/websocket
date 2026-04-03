package cloud.apposs.websocket.validator.checker;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 被注释的元素必须是一个ID数字（数字 > 0L），一般是通过{@link cloud.apposs.util.IdWorker}生成的数值
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Id {
    /**
     * 是否强制需要此值
     */
    boolean require() default true;

    /**
     * 最大值
     */
    long max() default Long.MAX_VALUE;

    /**
     * 错误消息输出
     */
    String message() default "";
}
