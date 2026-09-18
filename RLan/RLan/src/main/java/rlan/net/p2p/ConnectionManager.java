package rlan.net.p2p;

import rlan.config.Config;

public final class ConnectionManager {
    private final Config config;

    public ConnectionManager(Config config) {
        this.config = config;
    }

    public Config config() {
        return config;
    }
}
