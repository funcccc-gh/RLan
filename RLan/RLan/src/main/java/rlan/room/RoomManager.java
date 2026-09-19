package rlan.room;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class RoomManager {
    private final ConcurrentMap<UUID, Room> rooms = new ConcurrentHashMap<>();

    public Room create(String name, String password, String ownerId) {
        var room = new Room(UUID.randomUUID(), name, Room.hash(password), ownerId);
        rooms.put(room.id(), room);
        room.join(ownerId);
        return room;
    }

    public Optional<Room> find(UUID id) {
        return Optional.ofNullable(rooms.get(id));
    }

    public JoinResult join(UUID id, String password, String memberId) {
        var room = rooms.get(id);
        if (room == null) {
            return JoinResult.ROOM_NOT_FOUND;
        }
        if (!room.verifyPassword(password)) {
            return JoinResult.WRONG_PASSWORD;
        }
        if (room.isFull()) {
            return JoinResult.ROOM_FULL;
        }
        return room.join(memberId) ? JoinResult.SUCCESS : JoinResult.ALREADY_MEMBER;
    }

    public void leave(UUID id, String memberId) {
        var room = rooms.get(id);
        if (room != null) {
            room.leave(memberId);
        }
    }

    public void close(UUID id) {
        rooms.remove(id);
    }

    public boolean changePassword(UUID id, String ownerId, String newPassword) {
        var room = rooms.get(id);
        if (room == null || !room.ownerId().equals(ownerId)) {
            return false;
        }
        room.changePassword(newPassword);
        return true;
    }

    public int count() {
        return rooms.size();
    }
}
