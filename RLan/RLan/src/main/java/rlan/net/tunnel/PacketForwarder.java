package rlan.net.tunnel;

import rlan.net.p2p.ConnectionManager;
import rlan.protocol.MessageType;
import rlan.protocol.Packet;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Optional;
import java.util.function.Consumer;

public final class PacketForwarder {
    private final VirtualLan virtualLan;
    private final ConnectionManager connectionManager;
    private final Consumer<IpPacket> onLocalDeliver;

    public PacketForwarder(VirtualLan virtualLan, ConnectionManager connectionManager, Consumer<IpPacket> onLocalDeliver) {
        this.virtualLan = virtualLan;
        this.connectionManager = connectionManager;
        this.onLocalDeliver = onLocalDeliver;
    }

    public void send(IpPacket ipPacket) {
        var dst = ipPacket.destination();
        Optional<InetSocketAddress> target = virtualLan.resolve(dst);
        if (target.isPresent()) {
            connectionManager.send(new Packet(MessageType.DATA, ipPacket.encode()), target.get());
        }
    }

    public void onRemoteData(byte[] payload) {
        var ipPacket = IpPacket.wrap(payload);
        onLocalDeliver.accept(ipPacket);
    }

    public Consumer<Packet> messageHandler() {
        return packet -> {
            if (packet.type() == MessageType.DATA) {
                onRemoteData(packet.payload());
            }
        };
    }
}
