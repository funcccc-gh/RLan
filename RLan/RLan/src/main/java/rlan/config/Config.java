package rlan.config;

public final class Config {
    public enum Stack {
        IPV4_ONLY,
        IPV6_ONLY,
        DUAL
    }

    private int listenPort = 0;
    private int maxDevices = 8;
    private Stack stack = Stack.DUAL;

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

    public Stack stack() {
        return stack;
    }

    public Config stack(Stack stack) {
        this.stack = stack;
        return this;
    }

    public boolean ipv4Enabled() {
        return stack != Stack.IPV6_ONLY;
    }

    public boolean ipv6Enabled() {
        return stack != Stack.IPV4_ONLY;
    }
}
