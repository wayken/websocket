package cloud.apposs.websocket.validator.checker;

import cloud.apposs.util.StrUtil;
import cloud.apposs.websocket.commandar.Commandar;
import cloud.apposs.websocket.validator.IChecker;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;

public class EmailChecker implements IChecker {
    @Override
    public Object check(Commandar commandar, Field field, Annotation annotation, Object value) {
        Email anno = (Email) annotation;
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
        String email = value.toString();
        if (!StrUtil.isEmail(email)) {
            // 输出异常信息
            if (StrUtil.isEmpty(anno.message())) {
                throw new IllegalArgumentException("invalid email parameter " + field.getName());
            } else {
                throw new IllegalArgumentException(anno.message());
            }
        }

        return value;
    }
}
