package cloud.apposs.websocket.validator;

import cloud.apposs.websocket.commandar.Commandar;
import cloud.apposs.websocket.validator.checker.Number;
import cloud.apposs.websocket.validator.checker.*;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

/**
 * OOP验证器，对模型对象内有添加{@link IChecker}注解的字段进行合法性校验，如果校验失败则抛出{@link IllegalArgumentException}异常
 * 参考：
 * <pre>
 *  https://blog.csdn.net/justry_deng/article/details/86571671
 *  https://www.cnblogs.com/360minitao/p/14147919.html
 * </pre>
 */
public final class Validator {
    /**
     * 字段解析器列表，业务方也可以自定义并添加
     */
    private static final Map<Class<? extends Annotation>, IChecker> checkers =
            new HashMap<Class<? extends Annotation>, IChecker>();
    static {
        checkers.put(NotNull.class, new NotNullChecker());
        checkers.put(NotEmpty.class, new NotEmptyChecker());
        checkers.put(NotBlank.class, new NotBlankChecker());
        checkers.put(Digits.class, new DigitsChecker());
        checkers.put(Digits64.class, new Digits64Checker());
        checkers.put(Number.class, new NumberChecker());
        checkers.put(Number64.class, new Number64Checker());
        checkers.put(Id.class, new IdChecker());
        checkers.put(Uuid.class, new UuidChecker());
        checkers.put(Bool.class, new BoolChecker());
        checkers.put(Length.class, new LengthChecker());
        checkers.put(Email.class, new EmailChecker());
        checkers.put(Mobile.class, new MobileChecker());
        checkers.put(Pattern.class, new PatternChecker());
    }

    /**
     * 注册自定义字段校验器
     *
     * @param annotationType 注解类型
     * @param checker        对应的校验器
     */
    public static void register(Class<? extends Annotation> annotationType, IChecker checker) {
        if (annotationType == null || checker == null) {
            throw new IllegalArgumentException("annotationType and checker must not be null");
        }
        checkers.put(annotationType, checker);
    }

    /**
     * 对 model 对象所有字段进行合法性校验，并将校验/转换后的值回写到字段
     * <p>
     * 遍历 model 所有声明字段（含父类），对每个带有已注册注解的字段调用对应 {@link IChecker} 进行校验。
     * 校验通过后将转换值通过反射回写，校验失败则抛出 {@link IllegalArgumentException}。
     *
     * @param  commandar 当前命令对象，包含了当前命令的所有信息，如命令名称，参数等
     * @param  model     待校验的模型对象，不能为 null
     * @throws IllegalArgumentException 任意字段校验不通过时抛出
     */
    public static void validate(Commandar commandar, Object model) {
        if (model == null) {
            throw new IllegalArgumentException("model must not be null");
        }
        Class<?> clazz = model.getClass();
        // 遍历当前类及所有父类的字段
        while (clazz != null && clazz != Object.class) {
            for (Field field : clazz.getDeclaredFields()) {
                handleFieldValidate(commandar, model, field);
            }
            clazz = clazz.getSuperclass();
        }
    }

    /**
     * 对单个字段执行所有已注册注解的校验，并将结果回写
     *
     * @param commandar 当前命令对象，包含了当前命令的所有信息，如命令名称，参数等
     * @param model     待校验的模型对象
     * @param field     待校验的字段
     */
    private static void handleFieldValidate(Commandar commandar, Object model, Field field) {
        Annotation[] annotations = field.getDeclaredAnnotations();
        if (annotations == null || annotations.length == 0) {
            return;
        }
        for (Annotation annotation : annotations) {
            IChecker checker = checkers.get(annotation.annotationType());
            if (checker == null) {
                continue;
            }
            boolean accessible = field.isAccessible();
            try {
                field.setAccessible(true);
                Object value = field.get(model);
                Object result = checker.check(commandar, field, annotation, value);
                // 将校验/转换后的值回写到字段
                field.set(model, result);
            } catch (IllegalArgumentException e) {
                throw e;
            } catch (Exception e) {
                throw new IllegalArgumentException("field " + field.getName() + " validation failed: " + e.getMessage(), e);
            } finally {
                field.setAccessible(accessible);
            }
        }
    }
}
