package cloud.apposs.websocket.validator;

import cloud.apposs.websocket.commandar.Commandar;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;

/**
 * 参数检查接口
 */
public interface IChecker {
    /**
     * 校验参数是否合法，并且返回对应转码后的值
     *
     * @param  commandar 当前命令对象，包含了当前命令的所有信息，如命令名称，参数等
     * @param  field 参数字段
     * @param  annotation 字段上的注解，和对应的IChecker匹配
     * @param  value 原始值
     * @return 校验后的值，即有可能前端没传参数，但依然可以返回注解上的默认值
     * @throws Exception 任何参数不合法均抛出对应异常
     */
    Object check(Commandar commandar, Field field, Annotation annotation, Object value) throws Exception;
}
