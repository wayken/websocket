package cloud.apposs.websocket.distributed.pubsub;

import cloud.apposs.websocket.WSConfig;

import java.util.UUID;

public abstract class AbstractPubSubService implements IPubSubService {
    protected final String nodeId = "distributed-pubsub-service:" + UUID.randomUUID().toString();

    protected final WSConfig configuration;

    protected AbstractPubSubService(WSConfig configuration) {
        this.configuration = configuration;
    }

    @Override
    public String getNodeId() {
        return nodeId;
    }
}
