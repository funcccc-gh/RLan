package rlan;

public final class Main {
    private Main() {
    }

    public static void main(String[] args) {
        if (CliMain.run(args)) return;
        CliMain.printUsage();
    }
}
