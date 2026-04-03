package cloud.apposs.websocket.validator.checker;

import cloud.apposs.util.StrUtil;
import cloud.apposs.websocket.commandar.Commandar;
import cloud.apposs.websocket.validator.IChecker;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;

public class NotBlankChecker implements IChecker {
    @Override
    public Object check(Commandar commandar, Field field, Annotation annotation, Object value) {
        NotBlank anno = (NotBlank) annotation;
        if (!anno.require() && StrUtil.isEmpty(value)) {
            return value;
        }

        if (StrUtil.isEmpty(value)) {
            // 输出异常信息
            if (StrUtil.isEmpty(anno.message())) {
                throw new IllegalArgumentException("require parameter " + field.getName() + " in commandar " + commandar);
            } else {
                throw new IllegalArgumentException(anno.message());
            }
        }

        return value.toString().trim();
    }
}
