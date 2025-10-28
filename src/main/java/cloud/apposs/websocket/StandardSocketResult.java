package cloud.apposs.websocket;

import cloud.apposs.util.Errno;

import java.io.Serializable;

/**
 * 标准WebSocket数据返回封装，响应错误码如下
 *{
 *   "success": true,
 *   "code": 0,
 *   "message": "success",
 *   "timestamp": 1590387266300,
 *   "result": {...}
 * }
 * 主要服务于业务响应输出及底层业务异常响应输出，各字段定义如下：
 * 1. success: 执行结果，方便前端直接判断，成功为true
 * 2. code: 错误码，必须依据{@link cloud.apposs.util.Errno}，当success为false时业务可由code判断是哪些逻辑错误
 * 3. message: 错误输出，{@link cloud.apposs.util.Errno}会自动输出该错误信息，前端主要是依据该字段来进行错误信息国际化输出
 * 4. timestamp: 输出时间戳，前端每次请求时响应的时间戳都必须是最新时间
 * 5. result: 响应数据，可以为自定义Model对象
 */
public class StandardSocketResult implements Serializable {
    private static final long serialVersionUID = 1L;

    public static final int CODE_SUCCESS = 0;

    // 执行是否成功，方便前端直接判断
    private final boolean success;

    // 响应码，0表示成功，其他表示失败
    private final int code;

    // 响应消息
    private final String message;

    // 响应时间戳
    private final long timestamp;

    // 响应数据
    private final Object data;

    public StandardSocketResult(boolean success, Errno errno) {
        this(success, errno.value(), errno.description(), null, System.currentTimeMillis());
    }

    public StandardSocketResult(boolean success, Errno errno, Object data) {
        this(success, errno.value(), errno.description(), data, System.currentTimeMillis());
    }

    public StandardSocketResult(boolean success, int code, String message, Object data, long timestamp) {
        this.success = success;
        this.code = code;
        this.message = message;
        this.data = data;
        this.timestamp = timestamp;
    }

    public static StandardSocketResult success() {
        return new StandardSocketResult(true, Errno.OK, null);
    }

    public static StandardSocketResult success(Object data) {
        return new StandardSocketResult(true, Errno.OK, data);
    }

    public static StandardSocketResult error(Errno errno) {
        return new StandardSocketResult(false, errno);
    }

    public static StandardSocketResult error(Errno errno, Object data) {
        return new StandardSocketResult(false, errno, data);
    }

    public boolean isSuccess() {
        return success;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public Object getData() {
        return data;
    }
}
