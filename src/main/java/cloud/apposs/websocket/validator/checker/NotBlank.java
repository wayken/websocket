package cloud.apposs.websocket.validator.checker;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 被注释的字符串的必须非空，即调用trim()后，长度必须大于0，接受方获取的参数也是trim后的字符串
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface NotBlank {
    /**
     * 是否强制需要此值
     */
    boolean require() default true;

    /**
     * 错误消息输出
     */
    String message() default "";
}
