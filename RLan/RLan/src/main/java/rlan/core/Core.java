package rlan.core;

import rlan.config.Config;
import rlan.net.p2p.ConnectionManager;
import rlan.room.RoomManager;

public final class Core {
    private final Config config;
    private final RoomManager roomManager;
    private final ConnectionManager connectionManager;

    public Core(Config config) {
        this.config = config;
        this.roomManager = new RoomManager();
        this.connectionManager = new ConnectionManager(config);
    }

    public Config config() {
        return config;
    }

    public RoomManager roomManager() {
        return roomManager;
    }

    public ConnectionManager connectionManager() {
        return connectionManager;
    }
}
