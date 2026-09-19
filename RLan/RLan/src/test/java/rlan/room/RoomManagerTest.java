package rlan.room;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RoomManagerTest {

    @Test
    void create_房主自动入房() {
        var mgr = new RoomManager();
        var room = mgr.create("room1", "pass", "owner1");
        assertThat(room.members()).contains("owner1");
        assertThat(room.ownerId()).isEqualTo("owner1");
    }

    @Test
    void join_正确密码加入成功() {
        var mgr = new RoomManager();
        var room = mgr.create("room1", "pass", "owner1");
        var result = mgr.join(room.id(), "pass", "user1");
        assertThat(result).isEqualTo(JoinResult.SUCCESS);
    }

    @Test
    void join_错误密码返回WRONG_PASSWORD() {
        var mgr = new RoomManager();
        var room = mgr.create("room1", "pass", "owner1");
        var result = mgr.join(room.id(), "wrong", "user1");
        assertThat(result).isEqualTo(JoinResult.WRONG_PASSWORD);
    }

    @Test
    void join_房间不存在返回ROOM_NOT_FOUND() {
        var mgr = new RoomManager();
        var result = mgr.join(java.util.UUID.randomUUID(), "pass", "user1");
        assertThat(result).isEqualTo(JoinResult.ROOM_NOT_FOUND);
    }

    @Test
    void join_房间满返回ROOM_FULL() {
        var mgr = new RoomManager();
        var room = mgr.create("room1", "pass", "owner1");
        for (int i = 1; i < Room.MAX_DEVICES; i++) {
            mgr.join(room.id(), "pass", "user" + i);
        }
        var result = mgr.join(room.id(), "pass", "overflow");
        assertThat(result).isEqualTo(JoinResult.ROOM_FULL);
    }

    @Test
    void close_移除房间() {
        var mgr = new RoomManager();
        var room = mgr.create("room1", "pass", "owner1");
        mgr.close(room.id());
        assertThat(mgr.find(room.id())).isEmpty();
    }
}
