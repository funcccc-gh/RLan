package rlan.room;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class RoomManager {
    private final ConcurrentMap<UUID, Room> rooms = new ConcurrentHashMap<>();

    public Room create(String name, String password) {
        var room = new Room(UUID.randomUUID(), name, password);
        rooms.put(room.id(), room);
        return room;
    }

    public Optional<Room> find(UUID id) {
        return Optional.ofNullable(rooms.get(id));
    }

    public boolean join(UUID id, String password, String memberId) {
        var room = rooms.get(id);
        if (room == null || !room.verifyPassword(password)) {
            return false;
        }
        return room.join(memberId);
    }

    public void close(UUID id) {
        rooms.remove(id);
    }
}
