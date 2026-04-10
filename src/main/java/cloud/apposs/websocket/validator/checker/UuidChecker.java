package cloud.apposs.websocket.validator.checker;

import cloud.apposs.util.StrUtil;
import cloud.apposs.websocket.commandar.Commandar;
import cloud.apposs.websocket.validator.IChecker;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.UUID;

public class UuidChecker implements IChecker {
    @Override
    public Object check(Commandar commandar, Field field, Annotation annotation, Object value) throws Exception {
        Uuid anno = (Uuid) annotation;
        if (!anno.require() && value == null) {
            return value;
        }

        if (value == null) {
            if (StrUtil.isEmpty(anno.message())) {
                throw new IllegalArgumentException("require parameter " + field.getName() + " in commandar " + commandar);
            } else {
                throw new IllegalArgumentException(anno.message());
            }
        }

        String uuid = value.toString();
        try {
            return UUID.fromString(uuid);
        } catch (IllegalArgumentException e) {
            if (StrUtil.isEmpty(anno.message())) {
                throw new IllegalArgumentException("invalid uuid parameter " + field.getName());
            } else {
                throw new IllegalArgumentException(anno.message());
            }
        }
    }
}
