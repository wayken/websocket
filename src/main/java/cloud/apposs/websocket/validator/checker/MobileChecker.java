package cloud.apposs.websocket.validator.checker;

import cloud.apposs.util.StrUtil;
import cloud.apposs.websocket.commandar.Commandar;
import cloud.apposs.websocket.validator.IChecker;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;

public class MobileChecker implements IChecker {
    @Override
    public Object check(Commandar commandar, Field field, Annotation annotation, Object value) {
        Mobile anno = (Mobile) annotation;
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

        String mobile = value.toString();
        if (!StrUtil.isMobile(mobile)) {
            // 输出异常信息
            if (StrUtil.isEmpty(anno.message())) {
                throw new IllegalArgumentException("invalid mobile parameter " + field.getName());
            } else {
                throw new IllegalArgumentException(anno.message());
            }
        }

        return value;
    }
}
