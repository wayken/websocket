package cloud.apposs.websocket.distributed.pubsub.message;

import cloud.apposs.websocket.distributed.pubsub.PubSubMessage;
import cloud.apposs.websocket.protocol.Packet;

public class BroadcastMessage extends PubSubMessage {
    private static final long serialVersionUID = 1092047718303934349L;

    private final String namespace;

    private final String room;

    private final Packet packet;

    public BroadcastMessage(String namespace, String room, Packet packet) {
        this.namespace = namespace;
        this.room = room;
        this.packet = packet;
    }

    public String getNamespace() {
        return namespace;
    }

    public String getRoom() {
        return room;
    }

    public Packet getPacket() {
        return packet;
    }
}
