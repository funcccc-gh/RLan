package rlan.net.p2p;

import java.net.InetSocketAddress;

public final class P2PNode {
    private final String id;
    private InetSocketAddress address;

    public P2PNode(String id, InetSocketAddress address) {
        this.id = id;
        this.address = address;
    }

    public String id() {
        return id;
    }

    public InetSocketAddress address() {
        return address;
    }

    public void address(InetSocketAddress address) {
        this.address = address;
    }
}
