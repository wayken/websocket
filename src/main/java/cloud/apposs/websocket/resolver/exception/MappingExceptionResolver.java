package cloud.apposs.websocket.resolver.exception;

import cloud.apposs.rest.ExceptionHandler;
import cloud.apposs.rest.WebExceptionResolver;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 将每个异常由具体的异常Handler解析处理，提升异常处理速度
 */
public class MappingExceptionResolver<R, P> implements WebExceptionResolver<R, P> {
    protected ExceptionHandler<R, P> defaultHandler;

    protected final Map<Class<? extends Throwable>, ExceptionHandler<R, P>> exceptionHandlerMapping =
            new ConcurrentHashMap<Class<? extends Throwable>, ExceptionHandler<R, P>>();

    public void addExceptionHandler(ExceptionHandler<R, P> handler) {
        exceptionHandlerMapping.put(handler.getExceptionType(), handler);
    }

    @Override
    public Object resolveHandlerException(R request, P response, Throwable throwable) {
        ExceptionHandler<R, P> handler = exceptionHandlerMapping.get(throwable.getClass());
        if (handler != null) {
            return handler.resloveException(request, response, throwable);
        }
        if (defaultHandler != null) {
            return defaultHandler.resloveException(request, response, throwable);
        }
        return null;
    }
}
