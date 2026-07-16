package cloud.apposs.websocket.resolver.exception;

import cloud.apposs.util.Errno;

public interface CommandExceptionHandler {
    Class<? extends Throwable> getExceptionType();

    Errno resloveException(String commandId, Throwable throwable);
}
