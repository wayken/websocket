package cloud.apposs.websocket.sample.interceptor;

import cloud.apposs.ioc.annotation.Component;
import cloud.apposs.react.React;
import cloud.apposs.rest.Handler;
import cloud.apposs.util.StrUtil;
import cloud.apposs.websocket.WSHttpRequest;
import cloud.apposs.websocket.WSHttpResponse;
import cloud.apposs.websocket.interceptor.HttpInterceptorAdapter;

@Component
public class HttpSampleInterceptor extends HttpInterceptorAdapter {
    @Override
    public React<Boolean> preHandle(WSHttpRequest request, WSHttpResponse response, Handler handler) throws Exception {
        return React.emitter(() -> {
            String authorization = request.getHeader("authorization");
            if (StrUtil.isEmpty(authorization)) {
                authorization = request.getBodyParam().getString("authorization");
            }
            return true;
        });
    }
}
