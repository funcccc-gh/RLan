package rlan.net.p2p;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.DatagramChannel;
import io.netty.channel.socket.nio.NioDatagramChannel;

import java.net.InetSocketAddress;

public final class P2pEndpoint {
    private final DatagramChannel channel;

    public P2pEndpoint(EventLoopGroup group, int bindPort, ChannelHandler handler) throws InterruptedException {
        var bootstrap = new Bootstrap();
        bootstrap.group(group)
                .channel(NioDatagramChannel.class)
                .option(ChannelOption.SO_BROADCAST, true)
                .handler(handler);
        this.channel = (DatagramChannel) bootstrap.bind(bindPort).sync().channel();
    }

    public DatagramChannel channel() {
        return channel;
    }

    public InetSocketAddress localAddress() {
        return channel.localAddress();
    }

    public void close() {
        channel.close();
    }
}
