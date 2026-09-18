package rlan.protocol;

public final class Packet {
    private final MessageType type;
    private final byte[] payload;

    public Packet(MessageType type, byte[] payload) {
        this.type = type;
        this.payload = payload;
    }

    public MessageType type() {
        return type;
    }

    public byte[] payload() {
        return payload;
    }
}
