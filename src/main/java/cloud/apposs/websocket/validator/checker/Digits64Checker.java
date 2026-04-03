package cloud.apposs.websocket.validator.checker;

import cloud.apposs.util.Parser;
import cloud.apposs.util.StrUtil;
import cloud.apposs.websocket.commandar.Commandar;
import cloud.apposs.websocket.validator.IChecker;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.security.spec.InvalidParameterSpecException;

public class Digits64Checker implements IChecker {
    @Override
    public Object check(Commandar commandar, Field field, Annotation annotation, Object value) throws Exception {
        Digits64 anno = (Digits64) annotation;
        // 不强制需要值时，同时也判断该值是否为空则直接跳过赋值
        if (!anno.require() && value == null) {
            return value;
        }

        if (value == null) {
            // 输出异常信息
            if (StrUtil.isEmpty(anno.message())) {
                throw new IllegalArgumentException("require parameter " + field.getName() + " in commandar " + commandar);
            } else {
                throw new IllegalArgumentException(anno.message());
            }
        }

        // 校验参数是合法并转换为对象需要的类型值
        try {
            long digits = Parser.parseLong(value.toString(), -1L);
            long max = anno.max();
            if (digits < 0 || digits > max) {
                throw new InvalidParameterSpecException(field.getName());
            }
            return digits;
        } catch (NumberFormatException e) {
            throw e;
        }
    }
}
