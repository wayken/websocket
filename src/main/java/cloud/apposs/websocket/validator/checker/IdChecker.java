package cloud.apposs.websocket.validator.checker;

import cloud.apposs.util.Parser;
import cloud.apposs.util.StrUtil;
import cloud.apposs.websocket.commandar.Commandar;
import cloud.apposs.websocket.validator.IChecker;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.security.spec.InvalidParameterSpecException;

public class IdChecker implements IChecker {
    @Override
    public Object check(Commandar commandar, Field field, Annotation annotation, Object value) throws Exception {
        Id anno = (Id) annotation;
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

        try {
            long digits = Parser.parseLong(value.toString(), -1L);
            long max = anno.max();
            if (digits <= 0 || digits > max) {
                throw new InvalidParameterSpecException(field.getName());
            }
            return digits;
        } catch (NumberFormatException e) {
            throw e;
        }
    }
}
