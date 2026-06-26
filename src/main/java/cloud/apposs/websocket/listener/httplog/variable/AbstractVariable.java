package cloud.apposs.websocket.listener.httplog.variable;

import cloud.apposs.rest.listener.httplog.variable.IVariable;
import cloud.apposs.websocket.WSHttpRequest;
import cloud.apposs.websocket.WSHttpResponse;

/**
 * 请求日志选项解析，各选项定义如下：
 * $http_xxx: http header信息日志
 * $remot_addr: 请求远程地址
 * $remot_port: 请求远程端口
 * $host: 请求域名
 * $request: 请求url
 * $status: 响应状态码
 */
public abstract class AbstractVariable implements IVariable<WSHttpRequest, WSHttpResponse> {
}
