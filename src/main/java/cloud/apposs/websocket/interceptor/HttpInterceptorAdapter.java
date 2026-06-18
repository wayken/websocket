package cloud.apposs.websocket.interceptor;

import cloud.apposs.rest.interceptor.HandlerInterceptorAdapter;
import cloud.apposs.websocket.WSHttpRequest;
import cloud.apposs.websocket.WSHttpResponse;

/**
 * HTTP请求拦截器适配器，业务方继承此类并添加@Component注解即可实现HTTP请求拦截，可用于鉴权、限流、日志等场景
 *
 * <pre>
 * {@code
 * @Component
 * public class AuthHttpInterceptor extends HttpInterceptorAdapter {
 *     @Override
 *     public React<Boolean> preHandle(WSHttpRequest request, WSHttpResponse response, Handler handler) throws Exception {
 *         String token = request.getHeader("Authorization");
 *         if (token == null) {
 *             return React.just(false);
 *         }
 *         return React.just(true);
 *     }
 * }
 * }
 * </pre>
 */
public class HttpInterceptorAdapter extends HandlerInterceptorAdapter<WSHttpRequest, WSHttpResponse> {
}
