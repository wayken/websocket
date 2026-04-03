package cloud.apposs.websocket.validator.checker;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 被注释的元素必须是一个数字，其值必须在可接受的范围内
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Number64 {
    /**
     * 是否强制需要此值
     */
    boolean require() default true;

    /**
     * 最小值
     */
    long min() default Long.MIN_VALUE;

    /**
     * 最大值
     */
    long max() default Long.MAX_VALUE;

    /**
     * 错误消息输出
     */
    String message() default "";
}
