package rlan.ui;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public final class App extends Application {
    private static final double WIDTH = 520;
    private static final double HEIGHT = 400;

    @Override
    public void start(Stage stage) {
        var view = new MainView();
        var scene = new Scene(view, WIDTH, HEIGHT);
        scene.getStylesheets().add(getClass().getResource("/rlan.css").toExternalForm());
        stage.setTitle("RLan - 远程局域网联机工具");
        stage.setScene(scene);
        stage.setMinWidth(WIDTH);
        stage.setMinHeight(HEIGHT);
        stage.show();
    }
}
