package cloud.apposs.websocket.listener.httplog;

import cloud.apposs.ioc.annotation.Component;
import cloud.apposs.logger.Logger;
import cloud.apposs.rest.Handler;
import cloud.apposs.rest.NoHandlerFoundException;
import cloud.apposs.rest.RestConfig;
import cloud.apposs.rest.listener.httplog.HandlerLogListener;
import cloud.apposs.websocket.WSHttpRequest;
import cloud.apposs.websocket.WSHttpResponse;
import cloud.apposs.websocket.WebSocketConstants;
import cloud.apposs.websocket.listener.httplog.variable.VariableParser;

import java.io.FileNotFoundException;

/**
 * 请求日志监听输出，支持自定义HTTP请求日志格式输出
 */
@Component
public class HttpLogHandlerListener extends HandlerLogListener<WSHttpRequest, WSHttpResponse> {
    @Override
    public void initialize(RestConfig config) {
        super.initialize(config);
        String logFormat = config.getHttpLogFormat();
        this.parser = new VariableParser(logFormat);
    }

    @Override
    public void setStartTime(WSHttpRequest request, WSHttpResponse response, Handler handler) {
        request.setAttribute(WebSocketConstants.REQUEST_ATTRIBUTE_START_TIME, System.currentTimeMillis());
    }

    @Override
    public void handlerComplete(WSHttpRequest request, WSHttpResponse response, Handler handler, Object result, Throwable cause) {
        if (!loggable || cause instanceof NoHandlerFoundException || cause instanceof FileNotFoundException) {
            return;
        }
        if (cause != null) {
            Logger.error(cause, parser.parse(request, response, handler, cause));
        } else {
            Logger.info(parser.parse(request, response, handler, cause));
        }
    }
}
