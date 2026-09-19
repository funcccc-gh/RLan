package rlan.net.p2p;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.DatagramPacket;
import rlan.protocol.MessageType;
import rlan.protocol.Packet;

import java.net.InetSocketAddress;
import java.util.function.BiConsumer;

public final class P2pMessageHandler extends SimpleChannelInboundHandler<DatagramPacket> {
    private final BiConsumer<Packet, InetSocketAddress> onMessage;

    public P2pMessageHandler(BiConsumer<Packet, InetSocketAddress> onMessage) {
        this.onMessage = onMessage;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, DatagramPacket msg) {
        var buf = msg.content();
        if (buf.readableBytes() < 5) return;
        int typeIndex = buf.readByte() & 0xFF;
        int length = buf.readInt();
        if (buf.readableBytes() < length) return;
        var types = MessageType.values();
        if (typeIndex >= types.length) return;
        byte[] payload = new byte[length];
        buf.readBytes(payload);
        onMessage.accept(new Packet(types[typeIndex], payload), msg.sender());
    }
}
