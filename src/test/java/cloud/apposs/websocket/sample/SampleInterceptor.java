package cloud.apposs.websocket.sample;

import cloud.apposs.ioc.annotation.Component;
import cloud.apposs.websocket.WSSession;
import cloud.apposs.websocket.commandar.Commandar;
import cloud.apposs.websocket.interceptor.CommandarInterceptorAdapter;
import cloud.apposs.websocket.protocol.HandshakeData;

import java.util.List;

@Component
public class SampleInterceptor extends CommandarInterceptorAdapter {
    @Override
    public boolean isAuthorized(HandshakeData data) throws Exception {
        System.out.println("SampleInterceptor Auth");
        return true;
    }

    @Override
    public boolean onCommand(Commandar commandar, WSSession session, List<Object> argument) {
        System.out.println("SampleInterceptor onCommand " + commandar.getPath());
        return true;
    }
}
