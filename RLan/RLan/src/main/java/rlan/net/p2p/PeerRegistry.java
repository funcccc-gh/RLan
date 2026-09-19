package rlan.net.p2p;

import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class PeerRegistry {
    private final ConcurrentMap<String, P2PNode> peers = new ConcurrentHashMap<>();

    public void register(P2PNode node) {
        peers.put(node.id(), node);
    }

    public Optional<P2PNode> find(String id) {
        return Optional.ofNullable(peers.get(id));
    }

    public Collection<P2PNode> all() {
        return peers.values();
    }

    public void updateAddress(String id, InetSocketAddress address) {
        var node = peers.get(id);
        if (node != null) {
            node.address(address);
        }
    }

    public void remove(String id) {
        peers.remove(id);
    }
}
