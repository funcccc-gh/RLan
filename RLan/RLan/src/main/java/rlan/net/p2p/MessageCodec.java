package rlan.net.p2p;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageCodec;
import rlan.protocol.MessageType;
import rlan.protocol.Packet;

import java.util.List;

public final class MessageCodec extends MessageToMessageCodec<ByteBuf, Packet> {
    @Override
    protected void encode(ChannelHandlerContext ctx, Packet msg, List<Object> out) {
        var buf = Unpooled.buffer(5 + msg.payload().length);
        buf.writeByte(msg.type().ordinal());
        buf.writeInt(msg.payload().length);
        buf.writeBytes(msg.payload());
        out.add(buf);
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf msg, List<Object> out) {
        if (msg.readableBytes() < 5) return;
        int typeIndex = msg.readByte() & 0xFF;
        int length = msg.readInt();
        if (msg.readableBytes() < length) return;
        var types = MessageType.values();
        if (typeIndex >= types.length) return;
        byte[] payload = new byte[length];
        msg.readBytes(payload);
        out.add(new Packet(types[typeIndex], payload));
    }
}
