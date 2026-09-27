package rlan.room;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public final class Room {
    public static final int DEFAULT_MAX_DEVICES = 8;

    private final UUID id;
    private final String name;
    private volatile String passwordHash;
    private volatile String ownerId;
    private volatile int maxDevices;
    private final Set<String> members = ConcurrentHashMap.newKeySet();
    private final ConcurrentLinkedQueue<String> joinOrder = new ConcurrentLinkedQueue<>();
    private final Set<String> muted = ConcurrentHashMap.newKeySet();
    private final long createdAt;

    public Room(UUID id, String name, String passwordHash, String ownerId) {
        this(id, name, passwordHash, ownerId, System.currentTimeMillis());
    }

    public Room(UUID id, String name, String passwordHash, String ownerId, long createdAt) {
        this.id = id;
        this.name = name;
        this.passwordHash = passwordHash;
        this.ownerId = ownerId;
        this.maxDevices = DEFAULT_MAX_DEVICES;
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

    public synchronized void renameOwner(String newOwnerId) {
        if (members.remove(ownerId)) {
            joinOrder.remove(ownerId);
            members.add(newOwnerId);
            joinOrder.add(newOwnerId);
        }
        this.ownerId = newOwnerId;
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
        return members.size() >= maxDevices;
    }

    public int maxDevices() {
        return maxDevices;
    }

    public void maxDevices(int max) {
        this.maxDevices = Math.max(1, Math.min(99, max));
    }

    public boolean isMuted(String memberId) {
        return muted.contains(memberId);
    }

    public Set<String> mutedMembers() {
        return java.util.Collections.unmodifiableSet(muted);
    }

    public void mute(String memberId) {
        muted.add(memberId);
    }

    public void unmute(String memberId) {
        muted.remove(memberId);
    }

    public boolean join(String memberId) {
        if (isFull()) {
            return false;
        }
        if (members.add(memberId)) {
            joinOrder.add(memberId);
            return true;
        }
        return false;
    }

    public void leave(String memberId) {
        members.remove(memberId);
        joinOrder.remove(memberId);
        muted.remove(memberId);
    }

    public List<String> membersByJoinOrder() {
        var result = new ArrayList<String>();
        for (var m : joinOrder) {
            if (members.contains(m)) {
                result.add(m);
            }
        }
        return Collections.unmodifiableList(result);
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
