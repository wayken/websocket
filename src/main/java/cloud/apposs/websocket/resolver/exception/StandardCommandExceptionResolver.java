package cloud.apposs.websocket.resolver.exception;

import cloud.apposs.guard.exception.BlockException;
import cloud.apposs.guard.exception.FlowBlockException;
import cloud.apposs.guard.exception.FuseBlockException;
import cloud.apposs.guard.exception.LimitKeyException;
import cloud.apposs.ioc.annotation.Component;
import cloud.apposs.util.Errno;

import java.io.FileNotFoundException;
import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class StandardCommandExceptionResolver implements CommandExceptionResolver {
    private final CommandExceptionHandler defaultHandler = new DefaultExceptionHandler();

    protected final Map<Class<? extends Throwable>, CommandExceptionHandler> exceptionHandlerMapping = new ConcurrentHashMap();

    public StandardCommandExceptionResolver() {
        this.addExceptionHandler(new IllegalArgumentExceptionHandler());
        this.addExceptionHandler(new FileFoundExceptionHandler());
        this.addExceptionHandler(new UnsupportedOperationExceptionHandler());
        this.addExceptionHandler(new IllegalAccessExceptionHandler());
        this.addExceptionHandler(new BlockExceptionHandler());
        this.addExceptionHandler(new FlowBlockExceptionHandler());
        this.addExceptionHandler(new FuseBlockExceptionHandler());
        this.addExceptionHandler(new LimitKeyExceptionHandler());
        this.addExceptionHandler(new SQLExceptionHandler());
    }

    public void addExceptionHandler(CommandExceptionHandler handler) {
        exceptionHandlerMapping.put(handler.getExceptionType(), handler);
    }

    @Override
    public Errno resolveCommandException(String commandId, Throwable throwable) {
        CommandExceptionHandler handler = exceptionHandlerMapping.get(throwable.getClass());
        if (handler != null) {
            return handler.resloveException(commandId, throwable);
        }
        if (defaultHandler != null) {
            return defaultHandler.resloveException(commandId, throwable);
        }
        return null;
    }

    static class DefaultExceptionHandler implements CommandExceptionHandler {
        @Override
        public Class<? extends Throwable> getExceptionType() {
            return Throwable.class;
        }

        @Override
        public Errno resloveException(String commandId, Throwable throwable) {
            return Errno.ERROR;
        }
    }

    static class IllegalArgumentExceptionHandler implements CommandExceptionHandler {
        @Override
        public Class<? extends Throwable> getExceptionType() {
            return IllegalArgumentException.class;
        }

        @Override
        public Errno resloveException(String commandId, Throwable throwable) {
            return Errno.EARGUMENT;
        }
    }

    static class FileFoundExceptionHandler implements CommandExceptionHandler {
        @Override
        public Class<? extends Throwable> getExceptionType() {
            return FileNotFoundException.class;
        }

        @Override
        public Errno resloveException(String commandId, Throwable throwable) {
            return Errno.EFILE_NOT_FOUND;
        }
    }

    static class UnsupportedOperationExceptionHandler implements CommandExceptionHandler {
        @Override
        public Class<? extends Throwable> getExceptionType() {
            return UnsupportedOperationException.class;
        }

        @Override
        public Errno resloveException(String commandId, Throwable throwable) {
            return Errno.EUNSUPPORTED_OPERATION;
        }
    }

    static class IllegalAccessExceptionHandler implements CommandExceptionHandler {
        @Override
        public Class<? extends Throwable> getExceptionType() {
            return IllegalAccessException.class;
        }

        @Override
        public Errno resloveException(String commandId, Throwable throwable) {
            return Errno.EACCESS_DENIED;
        }
    }

    static class BlockExceptionHandler implements CommandExceptionHandler {
        @Override
        public Class<? extends Throwable> getExceptionType() {
            return BlockException.class;
        }

        @Override
        public Errno resloveException(String commandId, Throwable throwable) {
            return Errno.EBLOCK;
        }
    }

    static class FlowBlockExceptionHandler implements CommandExceptionHandler {
        @Override
        public Class<? extends Throwable> getExceptionType() {
            return FlowBlockException.class;
        }

        @Override
        public Errno resloveException(String commandId, Throwable throwable) {
            return Errno.EBLOCK;
        }
    }

    static class FuseBlockExceptionHandler implements CommandExceptionHandler {
        @Override
        public Class<? extends Throwable> getExceptionType() {
            return FuseBlockException.class;
        }

        @Override
        public Errno resloveException(String commandId, Throwable throwable) {
            return Errno.EBLOCK;
        }
    }

    static class LimitKeyExceptionHandler implements CommandExceptionHandler {
        @Override
        public Class<? extends Throwable> getExceptionType() {
            return LimitKeyException.class;
        }

        @Override
        public Errno resloveException(String commandId, Throwable throwable) {
            return Errno.EBLOCK;
        }
    }

    static class SQLExceptionHandler implements CommandExceptionHandler {
        @Override
        public Class<? extends Throwable> getExceptionType() {
            return SQLException.class;
        }

        @Override
        public Errno resloveException(String commandId, Throwable throwable) {
            return Errno.SQL_ERROR;
        }
    }
}
