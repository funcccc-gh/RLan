package rlan.config;

public final class Config {
    private int listenPort = 0;
    private int maxDevices = 8;
    private boolean ipv6Enabled = true;

    public int listenPort() {
        return listenPort;
    }

    public Config listenPort(int port) {
        this.listenPort = port;
        return this;
    }

    public int maxDevices() {
        return maxDevices;
    }

    public boolean ipv6Enabled() {
        return ipv6Enabled;
    }
}
