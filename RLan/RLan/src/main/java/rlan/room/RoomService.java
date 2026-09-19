package rlan.room;

import rlan.net.p2p.ConnectionManager;
import rlan.protocol.MessageType;
import rlan.protocol.Packet;

import java.net.InetSocketAddress;
import java.util.UUID;
import java.util.function.Consumer;

public final class RoomService {
    private final RoomManager roomManager;
    private final ConnectionManager connectionManager;
    private final String selfId;

    public RoomService(RoomManager roomManager, ConnectionManager connectionManager, String selfId) {
        this.roomManager = roomManager;
        this.connectionManager = connectionManager;
        this.selfId = selfId;
    }

    public Room createRoom(String name, String password) {
        return roomManager.create(name, password, selfId);
    }

    public JoinResult joinRoom(UUID roomId, String password) {
        return roomManager.join(roomId, password, selfId);
    }

    public void sendCreateRoom(String name, String password, InetSocketAddress target) {
        var msg = new RoomMessage(RoomMessage.Type.CREATE_ROOM, null, name, password, selfId);
        connectionManager.send(new Packet(MessageType.JOIN, msg.encode()), target);
    }

    public void sendJoinRoom(UUID roomId, String password, InetSocketAddress target) {
        var msg = new RoomMessage(RoomMessage.Type.JOIN_ROOM, roomId, null, password, selfId);
        connectionManager.send(new Packet(MessageType.JOIN, msg.encode()), target);
    }

    public void sendLeaveRoom(UUID roomId, InetSocketAddress target) {
        var msg = new RoomMessage(RoomMessage.Type.LEAVE_ROOM, roomId, null, null, selfId);
        connectionManager.send(new Packet(MessageType.LEAVE, msg.encode()), target);
    }

    public Consumer<Packet> messageHandler() {
        return packet -> {
            if (packet.type() != MessageType.JOIN && packet.type() != MessageType.LEAVE) {
                return;
            }
            var msg = RoomMessage.decode(packet.payload());
            if (msg == null) {
                return;
            }
            handle(msg);
        };
    }

    private void handle(RoomMessage msg) {
        switch (msg.type()) {
            case CREATE_ROOM -> {
                if (msg.name() != null && msg.password() != null) {
                    roomManager.create(msg.name(), msg.password(), msg.memberId());
                }
            }
            case JOIN_ROOM -> {
                if (msg.roomId() != null && msg.password() != null) {
                    roomManager.join(msg.roomId(), msg.password(), msg.memberId());
                }
            }
            case LEAVE_ROOM -> {
                if (msg.roomId() != null) {
                    roomManager.leave(msg.roomId(), msg.memberId());
                }
            }
            default -> {
            }
        }
    }
}
