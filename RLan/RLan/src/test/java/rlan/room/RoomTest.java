package rlan.room;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RoomTest {

    @Test
    void verifyPassword_正确密码返回true() {
        var room = new Room(java.util.UUID.randomUUID(), "test", Room.hash("secret"), "owner");
        assertThat(room.verifyPassword("secret")).isTrue();
    }

    @Test
    void verifyPassword_错误密码返回false() {
        var room = new Room(java.util.UUID.randomUUID(), "test", Room.hash("secret"), "owner");
        assertThat(room.verifyPassword("wrong")).isFalse();
    }

    @Test
    void hash_相同密码哈希一致() {
        assertThat(Room.hash("abc")).isEqualTo(Room.hash("abc"));
    }

    @Test
    void hash_不同密码哈希不同() {
        assertThat(Room.hash("abc")).isNotEqualTo(Room.hash("xyz"));
    }

    @Test
    void join_未满时加入成功() {
        var room = new Room(java.util.UUID.randomUUID(), "test", Room.hash("p"), "owner");
        assertThat(room.join("user1")).isTrue();
        assertThat(room.memberCount()).isEqualTo(1);
    }

    @Test
    void join_重复成员返回false() {
        var room = new Room(java.util.UUID.randomUUID(), "test", Room.hash("p"), "owner");
        room.join("user1");
        assertThat(room.join("user1")).isFalse();
    }

    @Test
    void isFull_达到8人时为满() {
        var room = new Room(java.util.UUID.randomUUID(), "test", Room.hash("p"), "owner");
        for (int i = 0; i < Room.MAX_DEVICES; i++) {
            room.join("user" + i);
        }
        assertThat(room.isFull()).isTrue();
        assertThat(room.join("extra")).isFalse();
    }

    @Test
    void leave_移除成员() {
        var room = new Room(java.util.UUID.randomUUID(), "test", Room.hash("p"), "owner");
        room.join("user1");
        room.leave("user1");
        assertThat(room.contains("user1")).isFalse();
        assertThat(room.memberCount()).isZero();
    }
}
