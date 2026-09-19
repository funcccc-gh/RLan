package rlan.net.tunnel;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class IpPacketTest {

    @Test
    void version_解析IPv4包() {
        byte[] raw = new byte[20];
        raw[0] = 0x45;
        var packet = new IpPacket(raw);
        assertThat(packet.version()).isEqualTo(4);
    }

    @Test
    void version_解析IPv6包() {
        byte[] raw = new byte[40];
        raw[0] = 0x60;
        var packet = new IpPacket(raw);
        assertThat(packet.version()).isEqualTo(6);
    }

    @Test
    void source和Destination_解析IPv4地址() {
        byte[] raw = new byte[20];
        raw[0] = 0x45;
        raw[12] = 10;
        raw[13] = (byte) 200;
        raw[14] = 0;
        raw[15] = 2;
        raw[16] = 10;
        raw[17] = (byte) 200;
        raw[18] = 0;
        raw[19] = 3;
        var packet = new IpPacket(raw);
        assertThat(packet.source().getHostAddress()).isEqualTo("10.200.0.2");
        assertThat(packet.destination().getHostAddress()).isEqualTo("10.200.0.3");
    }

    @Test
    void totalLength_解析IPv4总长度() {
        byte[] raw = new byte[20];
        raw[0] = 0x45;
        raw[2] = 0x01;
        raw[3] = (byte) 0xF4;
        var packet = new IpPacket(raw);
        assertThat(packet.totalLength()).isEqualTo(500);
    }

    @Test
    void headerLength_解析IPv4头长度() {
        byte[] raw = new byte[20];
        raw[0] = 0x45;
        var packet = new IpPacket(raw);
        assertThat(packet.headerLength()).isEqualTo(20);
    }
}
