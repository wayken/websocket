package cloud.apposs.websocket.resolver.view;

import cloud.apposs.ioc.annotation.Component;
import cloud.apposs.rest.RestConfig;
import cloud.apposs.rest.view.ViewResolver;
import cloud.apposs.util.StandardResult;
import cloud.apposs.websocket.WSHttpRequest;
import cloud.apposs.websocket.WSHttpResponse;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.charset.Charset;

/**
 * HTTP视图渲染器，将Handler返回结果以JSON格式输出到HTTP响应
 */
@Component
public class HttpViewResolver implements ViewResolver<WSHttpRequest, WSHttpResponse> {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private Charset charset = Charset.forName("UTF-8");

    @Override
    public ViewResolver<WSHttpRequest, WSHttpResponse> build(RestConfig config) {
        if (config != null && config.getCharset() != null) {
            this.charset = Charset.forName(config.getCharset());
        }
        return this;
    }

    @Override
    public boolean supports(WSHttpRequest request, WSHttpResponse response, Object result) {
        return true;
    }

    @Override
    public boolean isCompleted(WSHttpRequest request, WSHttpResponse response, Object result) {
        return true;
    }

    @Override
    public void render(WSHttpRequest request, WSHttpResponse response, Object result, boolean flush) throws Exception {
        byte[] content;
        if (result instanceof StandardResult) {
            content = ((StandardResult) result).toJson().getBytes(charset);
        } else if (result instanceof String) {
            content = ((String) result).getBytes(charset);
        } else {
            content = MAPPER.writeValueAsBytes(result);
        }
        response.setContentType("application/json; charset=" + charset.name());
        response.write(content, true);
    }
}
