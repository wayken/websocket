package cloud.apposs.websocket.netty;

import cloud.apposs.rest.Handler;
import cloud.apposs.rest.IGuardProcess;
import cloud.apposs.rest.IHandlerProcess;
import cloud.apposs.websocket.WSHttpRequest;
import cloud.apposs.websocket.WSHttpResponse;

import java.util.Map;

/**
 * 将WSHttpRequest/WSHttpResponse适配到Restful框架的IHandlerProcess接口
 */
public class NettyHandlerProcess implements IHandlerProcess<WSHttpRequest, WSHttpResponse> {
    private static final String ATTR_VARIABLES = "_ws_http_variables";

    @Override
    public String getRequestMethod(WSHttpRequest request, WSHttpResponse response) {
        return request.getMethod();
    }

    @Override
    public String getRequestPath(WSHttpRequest request, WSHttpResponse response) {
        return request.getPath();
    }

    @Override
    public String getRequestHost(WSHttpRequest request, WSHttpResponse response) {
        return request.getHost();
    }

    @Override
    @SuppressWarnings("unchecked")
    public void processVariable(WSHttpRequest request, WSHttpResponse response, Map<String, String> variables) {
        request.setAttribute(ATTR_VARIABLES, variables);
    }

    @Override
    public void processHandler(WSHttpRequest request, WSHttpResponse response, Handler handler) {
    }

    @Override
    public IGuardProcess<WSHttpRequest, WSHttpResponse> getGuardProcess() {
        return null;
    }

    @Override
    public void markAsync(WSHttpRequest request, WSHttpResponse response) {
    }

    @SuppressWarnings("unchecked")
    public static Map<String, String> getVariables(WSHttpRequest request) {
        return (Map<String, String>) request.getAttribute(ATTR_VARIABLES);
    }
}
