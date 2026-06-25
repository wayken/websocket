package cloud.apposs.websocket.resolver.parameter;

import cloud.apposs.ioc.annotation.Component;
import cloud.apposs.rest.annotation.Order;
import cloud.apposs.rest.parameter.BodyParameterResolver;
import cloud.apposs.rest.parameter.Parameter;
import cloud.apposs.util.Param;
import cloud.apposs.websocket.WSHttpRequest;
import cloud.apposs.websocket.WSHttpResponse;
import cloud.apposs.websocket.netty.NettyHandlerProcess;

import java.util.Map;

/**
 * 解析@Model注解参数，从HTTP请求体（JSON/表单）中反序列化为POJO对象
 */
@Order(1)
@Component
public class HttpModelParameterResolver extends BodyParameterResolver<WSHttpRequest, WSHttpResponse> {
    @Override
    public Param getParameterValues(Parameter parameter, WSHttpRequest request, WSHttpResponse response) throws Exception {
        Param param = new Param();
        // 解析查询参数
        Map<String, String> parameters = request.getParameters();
        if (parameters != null) {
            param.putAll(parameters);
        }
        // 解析路径变量
        Map<String, String> variables = NettyHandlerProcess.getVariables(request);
        if (variables != null) {
            param.putAll(variables);
        }
        // 解析请求体
        Param bodyParam = request.getBodyParam();
        if (bodyParam != null) {
            param.putAll(bodyParam);
        }
        return param;
    }
}
