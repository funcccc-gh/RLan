package rlan.net.p2p;

import io.netty.buffer.Unpooled;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramPacket;
import rlan.config.Config;
import rlan.net.stun.StunClient;
import rlan.protocol.Packet;

import java.net.InetSocketAddress;
import java.util.function.BiConsumer;

public final class ConnectionManager {
    private final Config config;
    private final EventLoopGroup group = new NioEventLoopGroup();
    private final PeerRegistry registry = new PeerRegistry();
    private P2pEndpoint endpoint;
    private InetSocketAddress publicAddress;

    public ConnectionManager(Config config) {
        this.config = config;
    }

    public Config config() {
        return config;
    }

    public PeerRegistry registry() {
        return registry;
    }

    public InetSocketAddress localAddress() {
        return endpoint == null ? null : endpoint.localAddress();
    }

    public InetSocketAddress publicAddress() {
        return publicAddress;
    }

    public void start(BiConsumer<Packet, InetSocketAddress> onMessage) throws InterruptedException {
        this.endpoint = new P2pEndpoint(group, config.listenPort(), new P2pMessageHandler(onMessage));
    }

    public InetSocketAddress discoverPublicAddress(InetSocketAddress stunServer) throws Exception {
        if (endpoint == null) {
            throw new IllegalStateException("ConnectionManager 未启动");
        }
        var stun = new StunClient();
        this.publicAddress = stun.query(endpoint.channel(), stunServer, 5000);
        return publicAddress;
    }

    public void send(Packet packet, InetSocketAddress target) {
        if (endpoint == null) return;
        var buf = Unpooled.buffer(5 + packet.payload().length);
        buf.writeByte(packet.type().ordinal());
        buf.writeInt(packet.payload().length);
        buf.writeBytes(packet.payload());
        endpoint.channel().writeAndFlush(new DatagramPacket(buf, target));
    }

    public void broadcast(Packet packet) {
        for (var node : registry.all()) {
            send(packet, node.address());
        }
    }

    public void close() {
        if (endpoint != null) {
            endpoint.close();
        }
        group.shutdownGracefully();
    }
}
