package cloud.apposs.websocket.validator.checker;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 被注释的元素必须符合指定的多个正则表达式中的其一或者多个
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Pattern {
    /**
     * 是否强制需要此值
     */
    boolean require() default true;

    /**
     * 正则表达式列表
     */
    String[] regex();

    /**
     * 是否为多个正则表达式匹配其一即可，
     * true为匹配其一即可，false为多个匹配才算成功
     */
    boolean xor() default true;

    /**
     * 错误消息输出
     */
    String message() default "";
}
