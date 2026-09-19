package rlan.room;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class Room {
    public static final int MAX_DEVICES = 8;

    private final UUID id;
    private final String name;
    private volatile String passwordHash;
    private final String ownerId;
    private final Set<String> members = ConcurrentHashMap.newKeySet();
    private final long createdAt;

    public Room(UUID id, String name, String passwordHash, String ownerId) {
        this(id, name, passwordHash, ownerId, System.currentTimeMillis());
    }

    public Room(UUID id, String name, String passwordHash, String ownerId, long createdAt) {
        this.id = id;
        this.name = name;
        this.passwordHash = passwordHash;
        this.ownerId = ownerId;
        this.createdAt = createdAt;
    }

    public UUID id() {
        return id;
    }

    public String name() {
        return name;
    }

    public String ownerId() {
        return ownerId;
    }

    public long createdAt() {
        return createdAt;
    }

    public boolean hasPassword() {
        return passwordHash != null && !passwordHash.isEmpty();
    }

    public boolean verifyPassword(String input) {
        return passwordHash.equals(hash(input));
    }

    public void changePassword(String newPassword) {
        this.passwordHash = hash(newPassword);
    }

    public Set<String> members() {
        return Collections.unmodifiableSet(members);
    }

    public int memberCount() {
        return members.size();
    }

    public boolean isFull() {
        return members.size() >= MAX_DEVICES;
    }

    public boolean join(String memberId) {
        if (isFull()) {
            return false;
        }
        return members.add(memberId);
    }

    public void leave(String memberId) {
        members.remove(memberId);
    }

    public boolean contains(String memberId) {
        return members.contains(memberId);
    }

    public static String hash(String password) {
        try {
            var md = MessageDigest.getInstance("SHA-256");
            var bytes = md.digest(password.getBytes(StandardCharsets.UTF_8));
            var sb = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
