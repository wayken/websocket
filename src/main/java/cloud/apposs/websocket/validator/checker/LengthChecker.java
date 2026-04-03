package cloud.apposs.websocket.validator.checker;

import cloud.apposs.util.StrUtil;
import cloud.apposs.websocket.commandar.Commandar;
import cloud.apposs.websocket.validator.IChecker;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;

public class LengthChecker implements IChecker {
    @Override
    public Object check(Commandar commandar, Field field, Annotation annotation, Object value) throws Exception {
        Length anno = (Length) annotation;
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

        String valueStr = value.toString();
        if (anno.trim()) {
            valueStr = valueStr.trim();
        }
        int length = valueStr.length();
        int min = anno.min();
        int max = anno.max();
        if (length < min || length > max) {
            throw new IllegalArgumentException("require parameter " + field.getName() +
                    " must be greater than " + min + " and less than " + max);
        }
        return valueStr;
    }
}
