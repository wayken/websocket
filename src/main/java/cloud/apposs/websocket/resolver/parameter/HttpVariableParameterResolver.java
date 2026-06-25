package cloud.apposs.websocket.resolver.parameter;

import cloud.apposs.ioc.annotation.Component;
import cloud.apposs.rest.annotation.Order;
import cloud.apposs.rest.annotation.Variable;
import cloud.apposs.rest.parameter.Parameter;
import cloud.apposs.rest.parameter.VariableParameterResolver;
import cloud.apposs.websocket.WSHttpRequest;
import cloud.apposs.websocket.WSHttpResponse;
import cloud.apposs.websocket.netty.NettyHandlerProcess;

import java.util.HashMap;
import java.util.Map;

/**
 * 解析@Variable注解的路径参数和查询参数
 */
@Order(0)
@Component
public class HttpVariableParameterResolver extends VariableParameterResolver<WSHttpRequest, WSHttpResponse> {
    @Override
    public Map<String, String> getParameterVariables(Parameter parameter, WSHttpRequest request, WSHttpResponse response) {
        // 先从路径变量获取
        Map<String, String> variables = NettyHandlerProcess.getVariables(request);
        if (variables != null) {
            Variable variable = (Variable) parameter.getAnnotation();
            if (variables.containsKey(variable.value())) {
                return variables;
            }
        }
        // 再从查询参数获取
        Variable variable = (Variable) parameter.getAnnotation();
        String parameterName = variable.value();
        String value = request.getParameter(parameterName);
        if (value != null) {
            Map<String, String> result = new HashMap<>();
            result.put(parameterName, value);
            return result;
        }
        return variables;
    }

    @Override
    public boolean isParameterTypeSupports(Class<?> parameterType) {
        return String.class.isAssignableFrom(parameterType)
                || Integer.class.isAssignableFrom(parameterType)
                || int.class.isAssignableFrom(parameterType)
                || Long.class.isAssignableFrom(parameterType)
                || long.class.isAssignableFrom(parameterType)
                || Boolean.class.isAssignableFrom(parameterType)
                || boolean.class.isAssignableFrom(parameterType);
    }

    @Override
    public Object castParameterValue(String parameterValue) {
        return parameterValue;
    }
}
