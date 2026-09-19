package rlan.net.stun;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramChannel;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.nio.NioDatagramChannel;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.security.SecureRandom;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public final class StunClient {
    private static final int MAGIC_COOKIE = 0x2112A442;
    private static final short BINDING_REQUEST = 0x0001;
    private static final short BINDING_RESPONSE = 0x0101;
    private static final short ATTR_MAPPED_ADDRESS = 0x0001;
    private static final short ATTR_XOR_MAPPED_ADDRESS = 0x0020;

    private final EventLoopGroup group = new NioEventLoopGroup(1);
    private final SecureRandom random = new SecureRandom();

    public InetSocketAddress query(InetSocketAddress stunServer, long timeoutMillis) throws Exception {
        byte[] transactionId = new byte[12];
        random.nextBytes(transactionId);

        var latch = new CountDownLatch(1);
        var result = new AtomicReference<InetSocketAddress>();

        var bootstrap = new Bootstrap();
        bootstrap.group(group)
                .channel(NioDatagramChannel.class)
                .option(ChannelOption.SO_BROADCAST, true)
                .handler(new SimpleChannelInboundHandler<DatagramPacket>() {
                    @Override
                    protected void channelRead0(ChannelHandlerContext ctx, DatagramPacket msg) {
                        var addr = parseResponse(msg.content(), transactionId);
                        if (addr != null) {
                            result.set(addr);
                            latch.countDown();
                        }
                    }
                });

        var channel = (DatagramChannel) bootstrap.bind(0).sync().channel();
        channel.writeAndFlush(new DatagramPacket(buildBindingRequest(transactionId), stunServer));

        try {
            if (!latch.await(timeoutMillis, TimeUnit.MILLISECONDS)) {
                throw new IllegalStateException("STUN 查询超时");
            }
            return result.get();
        } finally {
            channel.close().sync();
        }
    }

    private ByteBuf buildBindingRequest(byte[] transactionId) {
        var buf = Unpooled.buffer(20);
        buf.writeShort(BINDING_REQUEST);
        buf.writeShort(0);
        buf.writeInt(MAGIC_COOKIE);
        buf.writeBytes(transactionId);
        return buf;
    }

    private InetSocketAddress parseResponse(ByteBuf buf, byte[] transactionId) {
        if (buf.readableBytes() < 20) return null;
        short type = buf.readShort();
        if (type != BINDING_RESPONSE) return null;
        buf.readShort();
        buf.readInt();
        byte[] tid = new byte[12];
        buf.readBytes(tid);

        while (buf.readableBytes() >= 4) {
            short attrType = buf.readShort();
            int attrLen = buf.readUnsignedShort();
            int nextIndex = buf.readerIndex() + attrLen;
            if (attrType == ATTR_XOR_MAPPED_ADDRESS) {
                return readXorMappedAddress(buf);
            } else if (attrType == ATTR_MAPPED_ADDRESS) {
                return readMappedAddress(buf);
            }
            buf.readerIndex(nextIndex);
        }
        return null;
    }

    private InetSocketAddress readXorMappedAddress(ByteBuf buf) {
        buf.readByte();
        buf.readByte();
        int port = buf.readUnsignedShort() ^ (MAGIC_COOKIE >>> 16);
        byte[] addr = new byte[4];
        buf.readBytes(addr);
        addr[0] ^= (byte) (MAGIC_COOKIE >>> 24);
        addr[1] ^= (byte) (MAGIC_COOKIE >>> 16);
        addr[2] ^= (byte) (MAGIC_COOKIE >>> 8);
        addr[3] ^= (byte) MAGIC_COOKIE;
        try {
            return new InetSocketAddress(InetAddress.getByAddress(addr), port);
        } catch (Exception e) {
            return null;
        }
    }

    private InetSocketAddress readMappedAddress(ByteBuf buf) {
        buf.readByte();
        buf.readByte();
        int port = buf.readUnsignedShort();
        byte[] addr = new byte[4];
        buf.readBytes(addr);
        try {
            return new InetSocketAddress(InetAddress.getByAddress(addr), port);
        } catch (Exception e) {
            return null;
        }
    }

    public void close() {
        group.shutdownGracefully();
    }
}
