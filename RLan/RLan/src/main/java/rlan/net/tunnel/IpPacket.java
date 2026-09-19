package rlan.net.tunnel;

import java.net.InetAddress;
import java.net.UnknownHostException;

public final class IpPacket {
    private final byte[] raw;

    public IpPacket(byte[] raw) {
        this.raw = raw;
    }

    public int version() {
        return (raw[0] >>> 4) & 0x0F;
    }

    public int protocol() {
        return version() == 4 ? (raw[9] & 0xFF) : (raw[6] & 0xFF);
    }

    public InetAddress source() {
        return version() == 4 ? address(12, 4) : address(8, 16);
    }

    public InetAddress destination() {
        return version() == 4 ? address(16, 4) : address(24, 16);
    }

    public byte[] raw() {
        return raw;
    }

    public int totalLength() {
        if (version() == 4) {
            return ((raw[2] & 0xFF) << 8) | (raw[3] & 0xFF);
        }
        return ((raw[4] & 0xFF) << 8) | (raw[5] & 0xFF);
    }

    public int headerLength() {
        if (version() == 4) {
            return (raw[0] & 0x0F) * 4;
        }
        return 40;
    }

    private InetAddress address(int offset, int length) {
        byte[] addr = new byte[length];
        System.arraycopy(raw, offset, addr, 0, length);
        try {
            return InetAddress.getByAddress(addr);
        } catch (UnknownHostException e) {
            throw new IllegalStateException(e);
        }
    }

    public static IpPacket wrap(byte[] payload) {
        return new IpPacket(payload);
    }

    public byte[] encode() {
        return raw;
    }
}
