package cloud.apposs.websocket.sample;

import cloud.apposs.ioc.annotation.Component;
import cloud.apposs.websocket.WSSession;
import cloud.apposs.websocket.commandar.Commandar;
import cloud.apposs.websocket.interceptor.CommandarInterceptorAdapter;
import cloud.apposs.websocket.protocol.HandshakeData;

@Component
public class SampleInterceptor extends CommandarInterceptorAdapter {
    @Override
    public boolean isAuthorized(HandshakeData data) throws Exception {
        System.out.println("SampleInterceptor Auth");
        return true;
    }

    @Override
    public boolean onEvent(Commandar commandar, WSSession session, Object argument) {
        System.out.println("SampleInterceptor onEvent " + commandar.getPath());
        return true;
    }
}
