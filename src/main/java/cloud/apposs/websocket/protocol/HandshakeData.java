package cloud.apposs.websocket.protocol;

import java.io.Serializable;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;

public class HandshakeData implements Serializable {
    private static final long serialVersionUID = 1896350300161819978L;

    private final Map<String, String> parameters;

    private InetSocketAddress remoteAddress;

    private InetSocketAddress localAddress;

    private String url;

    public HandshakeData(Map<String, String> parameters, InetSocketAddress address, InetSocketAddress local, String url) {
        super();
        this.parameters = parameters;
        this.remoteAddress = address;
        this.localAddress = local;
        this.url = url;
    }

    public Map<String, String> getParameters() {
        return parameters;
    }

    public InetSocketAddress getRemoteAddress() {
        return remoteAddress;
    }

    public InetSocketAddress getLocalAddress() {
        return localAddress;
    }

    public String getUrl() {
        return url;
    }
}
