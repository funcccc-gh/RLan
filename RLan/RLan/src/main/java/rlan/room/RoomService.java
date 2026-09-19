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
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private Consumer<ChatProtocol> onChatMessage;
    private Consumer<RoomMigration> onRoomMigrated;

    public static final class RoomMigration {
        public final UUID oldRoomId;
        public final UUID newRoomId;
        public final String newEncodedHandle;
        public final String newPassword;
        public final String roomName;

        public RoomMigration(UUID oldRoomId, UUID newRoomId, String newEncodedHandle, String newPassword, String roomName) {
            this.oldRoomId = oldRoomId;
            this.newRoomId = newRoomId;
            this.newEncodedHandle = newEncodedHandle;
            this.newPassword = newPassword;
            this.roomName = roomName;
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

    public Room createRoom(String name, String password) {
        var room = roomManager.create(name, password, selfId);
        ownedRooms.put(room.id(), true);
        memberAddresses.put(room.id(), new ConcurrentHashMap<>());
        return room;
    }

    public void joinRoomRemote(RoomHandle handle, String password, Consumer<RemoteJoinResult> callback) {
        pendingJoins.put(handle.id(), callback);
        ownerAddresses.put(handle.id(), handle.address());
        var msg = new RoomMessage(RoomMessage.Type.JOIN_ROOM, handle.id(), null, password, selfId);
        connectionManager.send(new Packet(MessageType.JOIN, msg.encode()), handle.address());
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

    public RoomHandle migrateRoom(UUID oldRoomId, String newPassword, String host, int port) {
        var oldRoom = roomManager.find(oldRoomId).orElse(null);
        if (oldRoom == null) {
            return null;
        }
        var roomName = oldRoom.name();
        var oldMembers = oldRoom.members();

        roomManager.close(oldRoomId);
        var newRoom = roomManager.create(roomName, newPassword, selfId);
        for (var member : oldMembers) {
            if (!member.equals(selfId)) {
                newRoom.join(member);
            }
        }
        ownedRooms.put(newRoom.id(), true);
        var oldMemberAddrs = memberAddresses.remove(oldRoomId);
        memberAddresses.put(newRoom.id(), oldMemberAddrs != null ? new ConcurrentHashMap<>(oldMemberAddrs) : new ConcurrentHashMap<>());
        ownedRooms.remove(oldRoomId);

        var newHandle = new RoomHandle(newRoom.id(), host, port);
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
                case CHAT -> {
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
            case ROOM_MIGRATED -> handleRoomMigrated(msg);
            case LEAVE_ROOM -> {
                if (msg.roomId() != null) {
                    roomManager.leave(msg.roomId(), msg.memberId());
                    var members = memberAddresses.get(msg.roomId());
                    if (members != null) {
                        members.remove(msg.memberId());
                    }
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

    private void handleRoomMigrated(RoomMessage msg) {
        if (onRoomMigrated != null && msg.name() != null && msg.password() != null) {
            onRoomMigrated.accept(new RoomMigration(
                    null,
                    msg.roomId(),
                    msg.name(),
                    msg.password(),
                    null
            ));
        }
    }
}
