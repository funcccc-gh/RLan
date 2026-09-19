package rlan.net.tunnel;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public final class VirtualLan {
    private final byte[] subnetPrefix;
    private final int prefixLen;
    private final AtomicInteger nextHost = new AtomicInteger(2);
    private final Map<String, InetAddress> memberToVip = new ConcurrentHashMap<>();
    private final Map<InetAddress, InetSocketAddress> vipToAddress = new ConcurrentHashMap<>();

    public VirtualLan(byte[] subnetPrefix, int prefixLen) {
        this.subnetPrefix = subnetPrefix;
        this.prefixLen = prefixLen;
    }

    public InetAddress allocate(String memberId) {
        var existing = memberToVip.get(memberId);
        if (existing != null) {
            return existing;
        }
        int host = nextHost.getAndIncrement();
        byte[] addr = subnetPrefix.clone();
        addr[addr.length - 1] = (byte) host;
        try {
            var vip = InetAddress.getByAddress(addr);
            memberToVip.put(memberId, vip);
            return vip;
        } catch (UnknownHostException e) {
            throw new IllegalStateException(e);
        }
    }

    public void bind(String memberId, InetSocketAddress physical) {
        var vip = memberToVip.get(memberId);
        if (vip != null) {
            vipToAddress.put(vip, physical);
        }
    }

    public Optional<InetSocketAddress> resolve(InetAddress vip) {
        return Optional.ofNullable(vipToAddress.get(vip));
    }

    public Optional<InetAddress> vipOf(String memberId) {
        return Optional.ofNullable(memberToVip.get(memberId));
    }

    public void release(String memberId) {
        var vip = memberToVip.remove(memberId);
        if (vip != null) {
            vipToAddress.remove(vip);
        }
    }

    public int prefixLen() {
        return prefixLen;
    }

    public static VirtualLan ipv4Subnet() {
        return new VirtualLan(new byte[]{10, (byte) 200, 0, 0}, 24);
    }

    public static VirtualLan ipv6Subnet() {
        byte[] prefix = new byte[16];
        prefix[0] = (byte) 0xFD;
        prefix[1] = 0x00;
        prefix[2] = 0x00;
        prefix[3] = 0x00;
        prefix[4] = 0x00;
        prefix[5] = 0x00;
        prefix[6] = 0x00;
        prefix[7] = 0x00;
        prefix[8] = 0x00;
        prefix[9] = 0x00;
        prefix[10] = 0x00;
        prefix[11] = 0x00;
        prefix[12] = 0x00;
        prefix[13] = 0x00;
        prefix[14] = 0x00;
        prefix[15] = 0x00;
        return new VirtualLan(prefix, 64);
    }
}
