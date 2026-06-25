package cloud.apposs.websocket.resolver.parameter;

import cloud.apposs.ioc.annotation.Component;
import cloud.apposs.rest.annotation.Order;
import cloud.apposs.rest.parameter.Parameter;
import cloud.apposs.rest.parameter.ParameterResolver;
import cloud.apposs.websocket.WSHttpRequest;
import cloud.apposs.websocket.WSHttpResponse;

/**
 * 解析Action方法中WSHttpRequest和WSHttpResponse类型的参数
 */
@Component
@Order(Integer.MIN_VALUE)
public class HttpParameterResolver implements ParameterResolver<WSHttpRequest, WSHttpResponse> {
    @Override
    public boolean supportsParameter(Parameter parameter) {
        Class<?> type = parameter.getType();
        return WSHttpRequest.class.isAssignableFrom(type)
                || WSHttpResponse.class.isAssignableFrom(type);
    }

    @Override
    public Object resolveArgument(Parameter parameter, WSHttpRequest request, WSHttpResponse response) throws Exception {
        Class<?> type = parameter.getType();
        if (WSHttpRequest.class.isAssignableFrom(type)) {
            return request;
        }
        if (WSHttpResponse.class.isAssignableFrom(type)) {
            return response;
        }
        return null;
    }
}
