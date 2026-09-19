package rlan.room;

import rlan.net.p2p.ConnectionManager;
import rlan.protocol.MessageType;
import rlan.protocol.Packet;

import java.net.InetSocketAddress;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public final class RoomService {
    private final RoomManager roomManager;
    private final ConnectionManager connectionManager;
    private final String selfId;
    private final ConcurrentMap<UUID, Consumer<RemoteJoinResult>> pendingJoins = new ConcurrentHashMap<>();

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
        return roomManager.create(name, password, selfId);
    }

    public void joinRoomRemote(RoomHandle handle, String password, Consumer<RemoteJoinResult> callback) {
        pendingJoins.put(handle.id(), callback);
        var msg = new RoomMessage(RoomMessage.Type.JOIN_ROOM, handle.id(), null, password, selfId);
        connectionManager.send(new Packet(MessageType.JOIN, msg.encode()), handle.address());
    }

    public void sendLeaveRoom(UUID roomId, InetSocketAddress target) {
        var msg = new RoomMessage(RoomMessage.Type.LEAVE_ROOM, roomId, null, null, selfId);
        connectionManager.send(new Packet(MessageType.LEAVE, msg.encode()), target);
    }

    public BiConsumer<Packet, InetSocketAddress> messageHandler() {
        return (packet, sender) -> {
            if (packet.type() != MessageType.JOIN && packet.type() != MessageType.LEAVE) {
                return;
            }
            var msg = RoomMessage.decode(packet.payload());
            if (msg == null) {
                return;
            }
            handle(msg, sender);
        };
    }

    private void handle(RoomMessage msg, InetSocketAddress sender) {
        switch (msg.type()) {
            case JOIN_ROOM -> handleJoinQuery(msg, sender);
            case JOIN_ACK -> handleJoinAck(msg);
            case JOIN_NAK -> handleJoinNak(msg);
            case LEAVE_ROOM -> {
                if (msg.roomId() != null) {
                    roomManager.leave(msg.roomId(), msg.memberId());
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
}
