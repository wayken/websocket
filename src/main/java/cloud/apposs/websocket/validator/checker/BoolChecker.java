package cloud.apposs.websocket.validator.checker;

import cloud.apposs.util.Parser;
import cloud.apposs.util.StrUtil;
import cloud.apposs.websocket.commandar.Commandar;
import cloud.apposs.websocket.validator.IChecker;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;

public class BoolChecker implements IChecker {
    @Override
    public Object check(Commandar commandar, Field field, Annotation annotation, Object value) {
        Bool anno = (Bool) annotation;
        // 如果没有传递参数则按注解的默认值返回
        if (!anno.require() && value == null) {
            return anno.value();
        }

        if (value == null) {
            // 输出异常信息
            if (StrUtil.isEmpty(anno.message())) {
                throw new IllegalArgumentException("require parameter " + field.getName() + " in commandar " + commandar);
            } else {
                throw new IllegalArgumentException(anno.message());
            }
        }

        if (!(value instanceof Boolean)) {
            // 输出异常信息
            if (StrUtil.isEmpty(anno.message())) {
                throw new IllegalArgumentException("parameter " + field.getName() + " should be boolean");
            } else {
                throw new IllegalArgumentException(anno.message());
            }
        }
        return value;
    }
}
