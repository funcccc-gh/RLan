package rlan.room;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class Room {
    private final UUID id;
    private final String name;
    private final String password;
    private final Set<String> members = ConcurrentHashMap.newKeySet();

    public Room(UUID id, String name, String password) {
        this.id = id;
        this.name = name;
        this.password = password;
    }

    public UUID id() {
        return id;
    }

    public String name() {
        return name;
    }

    public boolean verifyPassword(String input) {
        return password.equals(input);
    }

    public Set<String> members() {
        return Collections.unmodifiableSet(members);
    }

    public boolean join(String memberId) {
        return members.add(memberId);
    }

    public void leave(String memberId) {
        members.remove(memberId);
    }
}
