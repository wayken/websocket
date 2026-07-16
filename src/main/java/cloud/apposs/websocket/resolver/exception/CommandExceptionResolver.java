package cloud.apposs.websocket.resolver.exception;

import cloud.apposs.util.Errno;

public interface CommandExceptionResolver {
    /**
     * 解析命令异常
     *
     * @param commandId 命令ID
     * @param throwable 异常
     * @return 错误码
     */
    Errno resolveCommandException(String commandId, Throwable throwable);
}
