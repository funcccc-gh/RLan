package rlan.net.tunnel;

import org.junit.jupiter.api.Test;

import java.net.InetAddress;

import static org.assertj.core.api.Assertions.assertThat;

class VirtualLanTest {

    @Test
    void allocate_为成员分配虚拟IP() {
        var lan = VirtualLan.ipv4Subnet();
        var vip = lan.allocate("member1");
        assertThat(vip).isNotNull();
        assertThat(vip.getAddress()[0]).isEqualTo((byte) 10);
    }

    @Test
    void allocate_同一成员返回相同IP() {
        var lan = VirtualLan.ipv4Subnet();
        var vip1 = lan.allocate("member1");
        var vip2 = lan.allocate("member1");
        assertThat(vip1).isEqualTo(vip2);
    }

    @Test
    void allocate_不同成员不同IP() {
        var lan = VirtualLan.ipv4Subnet();
        var vip1 = lan.allocate("member1");
        var vip2 = lan.allocate("member2");
        assertThat(vip1).isNotEqualTo(vip2);
    }

    @Test
    void bind和resolve_映射虚拟IP到物理地址() throws Exception {
        var lan = VirtualLan.ipv4Subnet();
        lan.allocate("member1");
        var physical = new java.net.InetSocketAddress("192.168.1.100", 5000);
        lan.bind("member1", physical);
        var resolved = lan.resolve(lan.vipOf("member1").orElseThrow());
        assertThat(resolved).contains(physical);
    }

    @Test
    void release_移除成员映射() {
        var lan = VirtualLan.ipv4Subnet();
        lan.allocate("member1");
        lan.release("member1");
        assertThat(lan.vipOf("member1")).isEmpty();
    }

    @Test
    void ipv6Subnet_创建IPv6子网() {
        var lan = VirtualLan.ipv6Subnet();
        var vip = lan.allocate("member1");
        assertThat(vip.getAddress().length).isEqualTo(16);
        assertThat(vip.getAddress()[0]).isEqualTo((byte) 0xFD);
    }
}
