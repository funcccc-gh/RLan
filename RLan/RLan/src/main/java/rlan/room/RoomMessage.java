package rlan.room;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

public final class RoomMessage {
    public enum Type {
        CREATE_ROOM,
        JOIN_ROOM,
        LEAVE_ROOM,
        ROOM_INFO,
        JOIN_ACK,
        JOIN_NAK,
        MEMBER_LIST
    }

    private final Type type;
    private final UUID roomId;
    private final String name;
    private final String password;
    private final String memberId;

    public RoomMessage(Type type, UUID roomId, String name, String password, String memberId) {
        this.type = type;
        this.roomId = roomId;
        this.name = name;
        this.password = password;
        this.memberId = memberId;
    }

    public Type type() {
        return type;
    }

    public UUID roomId() {
        return roomId;
    }

    public String name() {
        return name;
    }

    public String password() {
        return password;
    }

    public String memberId() {
        return memberId;
    }

    public byte[] encode() {
        var nameBytes = name == null ? new byte[0] : name.getBytes(StandardCharsets.UTF_8);
        var pwBytes = password == null ? new byte[0] : password.getBytes(StandardCharsets.UTF_8);
        var idBytes = memberId == null ? new byte[0] : memberId.getBytes(StandardCharsets.UTF_8);
        int capacity = 1 + 16 + 4 + nameBytes.length + 4 + pwBytes.length + 4 + idBytes.length;
        var buf = ByteBuffer.allocate(capacity);
        buf.put((byte) type.ordinal());
        putUuid(buf, roomId);
        putBytes(buf, nameBytes);
        putBytes(buf, pwBytes);
        putBytes(buf, idBytes);
        return buf.array();
    }

    public static RoomMessage decode(byte[] data) {
        var buf = ByteBuffer.wrap(data);
        var types = Type.values();
        int typeIndex = buf.get() & 0xFF;
        if (typeIndex >= types.length) {
            return null;
        }
        var type = types[typeIndex];
        var roomId = getUuid(buf);
        var name = getString(buf);
        var password = getString(buf);
        var memberId = getString(buf);
        return new RoomMessage(type, roomId, name, password, memberId);
    }

    private static void putUuid(ByteBuffer buf, UUID uuid) {
        if (uuid == null) {
            buf.putLong(0).putLong(0);
        } else {
            buf.putLong(uuid.getMostSignificantBits());
            buf.putLong(uuid.getLeastSignificantBits());
        }
    }

    private static UUID getUuid(ByteBuffer buf) {
        long msb = buf.getLong();
        long lsb = buf.getLong();
        return new UUID(msb, lsb);
    }

    private static void putBytes(ByteBuffer buf, byte[] bytes) {
        buf.putInt(bytes.length);
        buf.put(bytes);
    }

    private static String getString(ByteBuffer buf) {
        int len = buf.getInt();
        if (len <= 0) {
            return null;
        }
        byte[] bytes = new byte[len];
        buf.get(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }
}
