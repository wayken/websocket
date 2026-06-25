package cloud.apposs.websocket.resolver.exception;

import cloud.apposs.guard.exception.BlockException;
import cloud.apposs.guard.exception.FlowBlockException;
import cloud.apposs.guard.exception.FuseBlockException;
import cloud.apposs.guard.exception.LimitKeyException;
import cloud.apposs.ioc.annotation.Component;
import cloud.apposs.react.OnSubscribeHandle;
import cloud.apposs.rest.ExceptionHandler;
import cloud.apposs.rest.MappingExceptionResolver;
import cloud.apposs.rest.NoHandlerFoundException;
import cloud.apposs.rest.ReadOnlyException;
import cloud.apposs.util.Errno;
import cloud.apposs.util.StandardResult;
import cloud.apposs.websocket.WSHttpRequest;
import cloud.apposs.websocket.WSHttpResponse;

import java.io.FileNotFoundException;
import java.sql.SQLException;

/**
 * 标准异常处理类，输出格式为{@link StandardResult}，
 * 若有公共异常则在此公共组件下添加对应的异常解析，
 * 如果属于业务自定义的异常，则继承此类并注解@Component，底层会自动选该业务异常实现类
 */
@Component
public class StandardExceptionResolver extends MappingExceptionResolver<WSHttpRequest, WSHttpResponse> {
    public StandardExceptionResolver() {
        this.defaultHandler = new DefaultExceptionHandler();
        this.addExceptionHandler(new IllegalArgumentExceptionHandler());
        this.addExceptionHandler(new ReadOnlyExceptionHandler());
        this.addExceptionHandler(new StandardResultExceptionHandler());
        this.addExceptionHandler(new NoHandlerFoundExceptionHandler());
        this.addExceptionHandler(new FileFoundExceptionHandler());
        this.addExceptionHandler(new UnsupportedOperationExceptionHandler());
        this.addExceptionHandler(new IllegalAccessExceptionHandler());
        this.addExceptionHandler(new BlockExceptionHandler());
        this.addExceptionHandler(new FlowBlockExceptionHandler());
        this.addExceptionHandler(new FuseBlockExceptionHandler());
        this.addExceptionHandler(new LimitKeyExceptionHandler());
        this.addExceptionHandler(new SQLExceptionHandler());
    }

    class DefaultExceptionHandler implements ExceptionHandler<WSHttpRequest, WSHttpResponse> {
        @Override
        public Class<? extends Throwable> getExceptionType() {
            return Throwable.class;
        }

        @Override
        public Object resloveException(WSHttpRequest request, WSHttpResponse response, Throwable throwable) {
            return StandardResult.error(Errno.ERROR);
        }
    }

    class IllegalArgumentExceptionHandler implements ExceptionHandler<WSHttpRequest, WSHttpResponse> {
        @Override
        public Class<? extends Throwable> getExceptionType() {
            return IllegalArgumentException.class;
        }

        @Override
        public Object resloveException(WSHttpRequest request, WSHttpResponse response, Throwable throwable) {
            return StandardResult.error(Errno.EARGUMENT);
        }
    }

    class ReadOnlyExceptionHandler implements ExceptionHandler<WSHttpRequest, WSHttpResponse> {
        @Override
        public Class<? extends Throwable> getExceptionType() {
            return ReadOnlyException.class;
        }

        @Override
        public Object resloveException(WSHttpRequest request, WSHttpResponse response, Throwable throwable) {
            return StandardResult.error(Errno.EREADONLY);
        }
    }

    class NoHandlerFoundExceptionHandler implements ExceptionHandler<WSHttpRequest, WSHttpResponse> {
        @Override
        public Class<? extends Throwable> getExceptionType() {
            return NoHandlerFoundException.class;
        }

        @Override
        public Object resloveException(WSHttpRequest request, WSHttpResponse response, Throwable throwable) {
            String description = String.format("request not found for uri %s",
                    ((NoHandlerFoundException) throwable).getPath());
            return StandardResult.error(new Errno(Errno.ENO_HANDLER.value(), description));
        }
    }

    class FileFoundExceptionHandler implements ExceptionHandler<WSHttpRequest, WSHttpResponse> {
        @Override
        public Class<? extends Throwable> getExceptionType() {
            return FileNotFoundException.class;
        }

        @Override
        public Object resloveException(WSHttpRequest request, WSHttpResponse response, Throwable throwable) {
            return StandardResult.error(Errno.EFILE_NOT_FOUND);
        }
    }

    class StandardResultExceptionHandler implements ExceptionHandler<WSHttpRequest, WSHttpResponse> {
        @Override
        public Class<? extends Throwable> getExceptionType() {
            return OnSubscribeHandle.StandardResultException.class;
        }

        @Override
        public Object resloveException(WSHttpRequest request, WSHttpResponse response, Throwable throwable) {
            OnSubscribeHandle.StandardResultException standardResultException = (OnSubscribeHandle.StandardResultException) throwable;
            return standardResultException.getResult();
        }
    }

    class UnsupportedOperationExceptionHandler implements ExceptionHandler<WSHttpRequest, WSHttpResponse> {
        @Override
        public Class<? extends Throwable> getExceptionType() {
            return UnsupportedOperationException.class;
        }

        @Override
        public Object resloveException(WSHttpRequest request, WSHttpResponse response, Throwable throwable) {
            return StandardResult.error(Errno.EUNSUPPORTED_OPERATION);
        }
    }

    class IllegalAccessExceptionHandler implements ExceptionHandler<WSHttpRequest, WSHttpResponse> {
        @Override
        public Class<? extends Throwable> getExceptionType() {
            return IllegalAccessException.class;
        }

        @Override
        public Object resloveException(WSHttpRequest request, WSHttpResponse response, Throwable throwable) {
            return StandardResult.error(Errno.EACCESS_DENIED);
        }
    }

    class BlockExceptionHandler implements ExceptionHandler<WSHttpRequest, WSHttpResponse> {
        @Override
        public Class<? extends Throwable> getExceptionType() {
            return BlockException.class;
        }

        @Override
        public Object resloveException(WSHttpRequest request, WSHttpResponse response, Throwable throwable) {
            return StandardResult.error(Errno.EBLOCK);
        }
    }

    class FlowBlockExceptionHandler implements ExceptionHandler<WSHttpRequest, WSHttpResponse> {
        @Override
        public Class<? extends Throwable> getExceptionType() {
            return FlowBlockException.class;
        }

        @Override
        public Object resloveException(WSHttpRequest request, WSHttpResponse response, Throwable throwable) {
            return StandardResult.error(Errno.EBLOCK);
        }
    }

    class FuseBlockExceptionHandler implements ExceptionHandler<WSHttpRequest, WSHttpResponse> {
        @Override
        public Class<? extends Throwable> getExceptionType() {
            return FuseBlockException.class;
        }

        @Override
        public Object resloveException(WSHttpRequest request, WSHttpResponse response, Throwable throwable) {
            return StandardResult.error(Errno.EBLOCK);
        }
    }

    class LimitKeyExceptionHandler implements ExceptionHandler<WSHttpRequest, WSHttpResponse> {
        @Override
        public Class<? extends Throwable> getExceptionType() {
            return LimitKeyException.class;
        }

        @Override
        public Object resloveException(WSHttpRequest request, WSHttpResponse response, Throwable throwable) {
            return StandardResult.error(Errno.EBLOCK);
        }
    }

    class SQLExceptionHandler implements ExceptionHandler<WSHttpRequest, WSHttpResponse> {
        @Override
        public Class<? extends Throwable> getExceptionType() {
            return SQLException.class;
        }

        @Override
        public Object resloveException(WSHttpRequest request, WSHttpResponse response, Throwable throwable) {
            return StandardResult.error(Errno.SQL_ERROR);
        }
    }
}
