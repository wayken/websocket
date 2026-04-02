package cloud.apposs.websocket.interceptor;

import cloud.apposs.websocket.WSSession;
import cloud.apposs.websocket.commandar.Commandar;
import cloud.apposs.websocket.protocol.HandshakeData;

import java.util.List;

public class CommandarInterceptorAdapter implements CommandarInterceptor {
	@Override
	public boolean isAuthorized(HandshakeData data) throws Exception {
		return true;
	}

	@Override
	public boolean onCommand(Commandar commandar, WSSession session, List<Object> argument) {
		return true;
	}

	@Override
	public void afterCompletion(Commandar commandar, WSSession session, Throwable throwable) {
	}
}
