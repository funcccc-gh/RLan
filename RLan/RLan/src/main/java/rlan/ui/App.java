package rlan.ui;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

public final class App extends Application {
    @Override
    public void start(Stage stage) {
        var root = new StackPane(new Label("RLan - 远程局域网联机工具"));
        var scene = new Scene(root, 480, 320);
        stage.setTitle("RLan");
        stage.setScene(scene);
        stage.show();
    }
}
