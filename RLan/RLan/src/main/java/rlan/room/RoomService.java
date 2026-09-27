package rlan.room;

import rlan.net.p2p.ConnectionManager;
import rlan.protocol.ChatProtocol;
import rlan.protocol.MessageType;
import rlan.protocol.Packet;

import java.net.InetSocketAddress;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public final class RoomService {
    private final RoomManager roomManager;
    private final ConnectionManager connectionManager;
    private final String selfId;
    private final ConcurrentMap<UUID, Consumer<RemoteJoinResult>> pendingJoins = new ConcurrentHashMap<>();
    private final ConcurrentMap<UUID, ConcurrentMap<String, InetSocketAddress>> memberAddresses = new ConcurrentHashMap<>();
    private final ConcurrentMap<UUID, InetSocketAddress> ownerAddresses = new ConcurrentHashMap<>();
    private final ConcurrentMap<UUID, Boolean> ownedRooms = new ConcurrentHashMap<>();
    private final ConcurrentMap<UUID, Boolean> selfMuted = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private final java.util.concurrent.atomic.AtomicInteger roomSeq = new java.util.concurrent.atomic.AtomicInteger(0);
    private Consumer<ChatProtocol> onChatMessage;
    private Consumer<RoomMigration> onRoomMigrated;
    private Consumer<String> onKicked;
    private BiConsumer<UUID, Boolean> onMuted;
    private Consumer<UUID> onRoomClosed;
    private BiConsumer<UUID, String> onMemberJoined;
    private BiConsumer<UUID, String> onMemberLeft;

    public static final class RoomMigration {
        public final UUID oldRoomId;
        public final UUID newRoomId;
        public final String newEncodedHandle;
        public final String newPassword;
        public final String roomName;
        public final String host;
        public final int port;

        public RoomMigration(UUID oldRoomId, UUID newRoomId, String newEncodedHandle, String newPassword, String roomName, String host, int port) {
            this.oldRoomId = oldRoomId;
            this.newRoomId = newRoomId;
            this.newEncodedHandle = newEncodedHandle;
            this.newPassword = newPassword;
            this.roomName = roomName;
            this.host = host;
            this.port = port;
        }
    }

    public static final class RemoteJoinResult {
        public final boolean success;
        public final String roomName;
        public final String error;

        private RemoteJoinResult(boolean success, String roomName, String error) {
            this.success = success;
            this.roomName = roomName;
            this.error = error;
        }

        public static RemoteJoinResult ok(String roomName) {
            return new RemoteJoinResult(true, roomName, null);
        }

        public static RemoteJoinResult fail(String error) {
            return new RemoteJoinResult(false, null, error);
        }
    }

    public RoomService(RoomManager roomManager, ConnectionManager connectionManager, String selfId) {
        this.roomManager = roomManager;
        this.connectionManager = connectionManager;
        this.selfId = selfId;
    }

    public RoomHandle createRoom(String name, String password, String host, int port) {
        int seq = roomSeq.incrementAndGet();
        var roomId = RoomHandle.deriveUuid(host, port, seq);
        var room = roomManager.create(name, password, selfId, roomId);
        ownedRooms.put(room.id(), true);
        memberAddresses.put(room.id(), new ConcurrentHashMap<>());
        return new RoomHandle(roomId, host, port, seq);
    }

    public RoomHandle createRoomWithId(String name, String password, UUID roomId, String host, int port, int seq) {
        var room = roomManager.create(name, password, selfId, roomId);
        ownedRooms.put(room.id(), true);
        memberAddresses.put(room.id(), new ConcurrentHashMap<>());
        return new RoomHandle(roomId, host, port, seq);
    }

    public void joinRoomRemote(RoomHandle handle, String password, Consumer<RemoteJoinResult> callback) {
        joinRoomRemote(handle, password, handle.address(), callback);
    }

    public void joinRoomRemote(RoomHandle handle, String password, InetSocketAddress target, Consumer<RemoteJoinResult> callback) {
        pendingJoins.put(handle.id(), callback);
        ownerAddresses.put(handle.id(), target);
        var msg = new RoomMessage(RoomMessage.Type.JOIN_ROOM, handle.id(), null, password, selfId);
        connectionManager.send(new Packet(MessageType.JOIN, msg.encode()), target);
        scheduler.schedule(() -> {
            var cb = pendingJoins.remove(handle.id());
            if (cb != null) {
                cb.accept(RemoteJoinResult.fail("查询超时：房主未响应，请检查房间 ID 和网络"));
            }
        }, 5, TimeUnit.SECONDS);
    }

    public void sendLeaveRoom(UUID roomId, InetSocketAddress target) {
        var msg = new RoomMessage(RoomMessage.Type.LEAVE_ROOM, roomId, null, null, selfId);
        connectionManager.send(new Packet(MessageType.LEAVE, msg.encode()), target);
        ownerAddresses.remove(roomId);
        memberAddresses.remove(roomId);
        ownedRooms.remove(roomId);
    }

    public boolean kickMember(UUID roomId, String memberId) {
        var room = roomManager.find(roomId).orElse(null);
        if (room == null || !room.ownerId().equals(selfId)) return false;
        var members = memberAddresses.get(roomId);
        if (members != null) {
            var addr = members.get(memberId);
            if (addr != null) {
                var msg = new RoomMessage(RoomMessage.Type.KICK, roomId, null, null, selfId);
                connectionManager.send(new Packet(MessageType.JOIN, msg.encode()), addr);
            }
            members.remove(memberId);
        }
        room.leave(memberId);
        return true;
    }

    public boolean muteMember(UUID roomId, String memberId, boolean mute) {
        var room = roomManager.find(roomId).orElse(null);
        if (room == null || !room.ownerId().equals(selfId)) return false;
        if (mute) room.mute(memberId); else room.unmute(memberId);
        var members = memberAddresses.get(roomId);
        if (members != null) {
            var addr = members.get(memberId);
            if (addr != null) {
                var msg = new RoomMessage(RoomMessage.Type.MUTE, roomId, null, mute ? "1" : "0", selfId);
                connectionManager.send(new Packet(MessageType.JOIN, msg.encode()), addr);
            }
        }
        return true;
    }

    public boolean setCapacity(UUID roomId, int capacity) {
        var room = roomManager.find(roomId).orElse(null);
        if (room == null || !room.ownerId().equals(selfId)) return false;
        room.maxDevices(capacity);
        var ordered = room.membersByJoinOrder();
        while (room.memberCount() > room.maxDevices()) {
            var toKick = ordered.remove(ordered.size() - 1);
            if (toKick.equals(selfId)) continue;
            kickMember(roomId, toKick);
        }
        return true;
    }

    public boolean closeRoom(UUID roomId) {
        var room = roomManager.find(roomId).orElse(null);
        if (room == null || !room.ownerId().equals(selfId)) return false;
        var members = memberAddresses.get(roomId);
        if (members != null) {
            var msg = new RoomMessage(RoomMessage.Type.ROOM_CLOSED, roomId, null, null, selfId);
            var packet = new Packet(MessageType.JOIN, msg.encode());
            for (var addr : members.values()) {
                connectionManager.send(packet, addr);
            }
        }
        roomManager.close(roomId);
        memberAddresses.remove(roomId);
        ownedRooms.remove(roomId);
        return true;
    }

    public void sendAudio(UUID roomId, byte[] data) {
        var proto = ChatProtocol.audio(roomId, selfId, data);
        dispatchChat(proto, roomId);
    }

    public void onKicked(Consumer<String> handler) {
        this.onKicked = handler;
    }

    public void onMuted(BiConsumer<UUID, Boolean> handler) {
        this.onMuted = handler;
    }

    public void onRoomClosed(Consumer<UUID> handler) {
        this.onRoomClosed = handler;
    }

    public void onMemberJoined(BiConsumer<UUID, String> handler) {
        this.onMemberJoined = handler;
    }

    public void onMemberLeft(BiConsumer<UUID, String> handler) {
        this.onMemberLeft = handler;
    }

    public boolean isSelfMuted(UUID roomId) {
        return selfMuted.getOrDefault(roomId, false);
    }

    public java.util.Set<String> getMembers(UUID roomId) {
        var room = roomManager.find(roomId).orElse(null);
        if (room == null) return java.util.Set.of();
        return room.members();
    }

    public boolean isOwner(UUID roomId) {
        return ownedRooms.containsKey(roomId);
    }

    public String getOwnerId(UUID roomId) {
        var room = roomManager.find(roomId).orElse(null);
        if (room == null) return null;
        return room.ownerId();
    }

    public boolean renameOwner(UUID roomId, String newOwnerId) {
        var room = roomManager.find(roomId).orElse(null);
        if (room == null || !room.ownerId().equals(selfId)) return false;
        room.renameOwner(newOwnerId);
        return true;
    }

    public boolean isMemberMuted(UUID roomId, String memberId) {
        var room = roomManager.find(roomId).orElse(null);
        if (room == null) return false;
        return room.isMuted(memberId);
    }

    public int getCapacity(UUID roomId) {
        var room = roomManager.find(roomId).orElse(null);
        if (room == null) return -1;
        return room.maxDevices();
    }

    public java.util.Set<String> getMutedMembers(UUID roomId) {
        var room = roomManager.find(roomId).orElse(null);
        if (room == null) return java.util.Set.of();
        return room.mutedMembers();
    }

    public java.util.Map<String, InetSocketAddress> getMemberAddresses(UUID roomId) {
        var addrs = memberAddresses.get(roomId);
        if (addrs == null) return java.util.Map.of();
        return java.util.Collections.unmodifiableMap(addrs);
    }

    public RoomHandle migrateRoom(UUID oldRoomId, String newPassword, String host, int port) {
        var oldRoom = roomManager.find(oldRoomId).orElse(null);
        if (oldRoom == null) {
            return null;
        }
        var roomName = oldRoom.name();
        var oldMembers = oldRoom.members();

        roomManager.close(oldRoomId);
        int seq = roomSeq.incrementAndGet();
        var newRoomId = RoomHandle.deriveUuid(host, port, seq);
        var newRoom = roomManager.create(roomName, newPassword, selfId, newRoomId);
        for (var member : oldMembers) {
            if (!member.equals(selfId)) {
                newRoom.join(member);
            }
        }
        ownedRooms.put(newRoom.id(), true);
        var oldMemberAddrs = memberAddresses.remove(oldRoomId);
        memberAddresses.put(newRoom.id(), oldMemberAddrs != null ? new ConcurrentHashMap<>(oldMemberAddrs) : new ConcurrentHashMap<>());
        ownedRooms.remove(oldRoomId);

        var newHandle = new RoomHandle(newRoom.id(), host, port, seq);
        var encodedHandle = newHandle.encode(newPassword);

        if (oldMemberAddrs != null) {
            var migrateMsg = new RoomMessage(RoomMessage.Type.ROOM_MIGRATED, newRoom.id(), encodedHandle, newPassword, selfId);
            var packet = new Packet(MessageType.JOIN, migrateMsg.encode());
            for (var addr : oldMemberAddrs.values()) {
                connectionManager.send(packet, addr);
            }
        }

        return newHandle;
    }

    public void sendChatText(UUID roomId, String content) {
        var proto = ChatProtocol.text(roomId, selfId, content);
        dispatchChat(proto, roomId);
    }

    public void sendChatImage(UUID roomId, String fileName, byte[] data) {
        var proto = ChatProtocol.image(roomId, selfId, fileName, data);
        dispatchChat(proto, roomId);
    }

    public void sendChatFile(UUID roomId, String fileName, byte[] data) {
        var proto = ChatProtocol.file(roomId, selfId, fileName, data);
        dispatchChat(proto, roomId);
    }

    private void dispatchChat(ChatProtocol proto, UUID roomId) {
        if (selfMuted.getOrDefault(roomId, false)) return;
        if (ownedRooms.containsKey(roomId)) {
            broadcastToMembers(proto, roomId);
        } else {
            var ownerAddr = ownerAddresses.get(roomId);
            if (ownerAddr != null) {
                connectionManager.send(new Packet(MessageType.CHAT, proto.encode()), ownerAddr);
            }
        }
    }

    private void broadcastToMembers(ChatProtocol proto, UUID roomId) {
        var members = memberAddresses.get(roomId);
        if (members == null) return;
        var packet = new Packet(MessageType.CHAT, proto.encode());
        for (var addr : members.values()) {
            connectionManager.send(packet, addr);
        }
    }

    public void onChatMessage(Consumer<ChatProtocol> handler) {
        this.onChatMessage = handler;
    }

    public void onRoomMigrated(Consumer<RoomMigration> handler) {
        this.onRoomMigrated = handler;
    }

    public BiConsumer<Packet, InetSocketAddress> messageHandler() {
        return (packet, sender) -> {
            switch (packet.type()) {
                case JOIN, LEAVE -> {
                    var msg = RoomMessage.decode(packet.payload());
                    if (msg != null) {
                        handle(msg, sender);
                    }
                }
                case CHAT, AUDIO -> {
                    var proto = ChatProtocol.decode(packet.payload());
                    if (proto != null) {
                        handleChat(proto, sender);
                    }
                }
                default -> {
                }
            }
        };
    }

    private void handle(RoomMessage msg, InetSocketAddress sender) {
        switch (msg.type()) {
            case JOIN_ROOM -> handleJoinQuery(msg, sender);
            case JOIN_ACK -> handleJoinAck(msg);
            case JOIN_NAK -> handleJoinNak(msg);
            case ROOM_MIGRATED -> handleRoomMigrated(msg, sender);
            case LEAVE_ROOM -> {
                if (msg.roomId() != null) {
                    roomManager.leave(msg.roomId(), msg.memberId());
                    var members = memberAddresses.get(msg.roomId());
                    if (members != null) {
                        members.remove(msg.memberId());
                        var leftMsg = new RoomMessage(RoomMessage.Type.MEMBER_LEFT, msg.roomId(), null, null, msg.memberId());
                        var packet = new Packet(MessageType.JOIN, leftMsg.encode());
                        for (var addr : members.values()) {
                            connectionManager.send(packet, addr);
                        }
                    }
                    if (onMemberLeft != null) {
                        onMemberLeft.accept(msg.roomId(), msg.memberId());
                    }
                }
            }
            case KICK -> {
                if (onKicked != null && msg.roomId() != null) {
                    onKicked.accept(msg.roomId().toString());
                }
            }
            case MUTE -> {
                if (msg.roomId() != null) {
                    boolean muted = "1".equals(msg.password());
                    selfMuted.put(msg.roomId(), muted);
                    if (onMuted != null) {
                        onMuted.accept(msg.roomId(), muted);
                    }
                }
            }
            case ROOM_CLOSED -> {
                if (onRoomClosed != null && msg.roomId() != null) {
                    onRoomClosed.accept(msg.roomId());
                }
            }
            case MEMBER_JOINED -> {
                if (onMemberJoined != null && msg.roomId() != null && msg.memberId() != null) {
                    onMemberJoined.accept(msg.roomId(), msg.memberId());
                }
            }
            case MEMBER_LEFT -> {
                if (onMemberLeft != null && msg.roomId() != null && msg.memberId() != null) {
                    onMemberLeft.accept(msg.roomId(), msg.memberId());
                }
            }
            default -> {
            }
        }
    }

    private void handleJoinQuery(RoomMessage msg, InetSocketAddress sender) {
        if (msg.roomId() == null || msg.password() == null) {
            return;
        }
        var result = roomManager.join(msg.roomId(), msg.password(), msg.memberId());
        if (result == JoinResult.SUCCESS || result == JoinResult.ALREADY_MEMBER) {
            var members = memberAddresses.computeIfAbsent(msg.roomId(), k -> new ConcurrentHashMap<>());
            members.put(msg.memberId(), sender);
            var room = roomManager.find(msg.roomId()).orElse(null);
            var name = room != null ? room.name() : "未知";
            var ack = new RoomMessage(RoomMessage.Type.JOIN_ACK, msg.roomId(), name, null, selfId);
            connectionManager.send(new Packet(MessageType.JOIN, ack.encode()), sender);

            if (result == JoinResult.SUCCESS && room != null) {
                var joinedMsg = new RoomMessage(RoomMessage.Type.MEMBER_JOINED, msg.roomId(), null, null, msg.memberId());
                var joinedPacket = new Packet(MessageType.JOIN, joinedMsg.encode());
                for (var e : members.entrySet()) {
                    if (!e.getKey().equals(msg.memberId())) {
                        connectionManager.send(joinedPacket, e.getValue());
                    }
                }
                for (var existing : room.membersByJoinOrder()) {
                    if (!existing.equals(msg.memberId()) && !existing.equals(selfId)) {
                        var existMsg = new RoomMessage(RoomMessage.Type.MEMBER_JOINED, msg.roomId(), null, null, existing);
                        connectionManager.send(new Packet(MessageType.JOIN, existMsg.encode()), sender);
                    }
                }
            }
        } else {
            var nak = new RoomMessage(RoomMessage.Type.JOIN_NAK, msg.roomId(), null, null, result.name());
            connectionManager.send(new Packet(MessageType.JOIN, nak.encode()), sender);
        }
    }

    private void handleJoinAck(RoomMessage msg) {
        var callback = pendingJoins.remove(msg.roomId());
        if (callback != null) {
            callback.accept(RemoteJoinResult.ok(msg.name()));
        }
    }

    private void handleJoinNak(RoomMessage msg) {
        var callback = pendingJoins.remove(msg.roomId());
        if (callback != null) {
            callback.accept(RemoteJoinResult.fail(msg.password() != null ? msg.password() : "加入失败"));
        }
    }

    private void handleChat(ChatProtocol proto, InetSocketAddress sender) {
        if (proto.senderId().equals(selfId)) {
            return;
        }
        if (ownedRooms.containsKey(proto.roomId())) {
            broadcastToMembers(proto, proto.roomId());
        }
        if (onChatMessage != null) {
            onChatMessage.accept(proto);
        }
    }

    private void handleRoomMigrated(RoomMessage msg, InetSocketAddress sender) {
        if (onRoomMigrated != null && msg.name() != null && msg.password() != null) {
            onRoomMigrated.accept(new RoomMigration(
                    null,
                    msg.roomId(),
                    msg.name(),
                    msg.password(),
                    null,
                    sender.getHostString(),
                    sender.getPort()
            ));
        }
    }
}
