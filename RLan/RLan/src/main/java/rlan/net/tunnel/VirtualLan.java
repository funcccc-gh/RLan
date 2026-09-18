package rlan.net.tunnel;

import java.net.InetAddress;

public final class VirtualLan {
    private final InetAddress subnet;

    public VirtualLan(InetAddress subnet) {
        this.subnet = subnet;
    }

    public InetAddress subnet() {
        return subnet;
    }
}
