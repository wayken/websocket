package cloud.apposs.websocket.listener.httplog.variable;

import cloud.apposs.rest.Handler;
import cloud.apposs.websocket.WSHttpRequest;
import cloud.apposs.websocket.WSHttpResponse;

import java.net.InetSocketAddress;

/**
 * 请求远程端口，对应参数：$remote_port
 */
public class RemotePortVariable extends AbstractVariable {
    @Override
    public String parse(WSHttpRequest request, WSHttpResponse response, Handler handler, Throwable t) {
        InetSocketAddress address = (InetSocketAddress) request.getRemoteAddr();
        return String.valueOf(address.getPort());
    }
}
