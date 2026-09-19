package rlan.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.UUID;
import java.util.function.Consumer;

public final class MainView extends VBox {
    private final TextField roomNameField = new TextField();
    private final TextField createPasswordField = new TextField();
    private final TextField joinIdField = new TextField();
    private final TextField joinPasswordField = new TextField();
    private final Label statusLabel = new Label("请选择创建房间或加入房间");

    private Consumer<RoomRequest> onCreateRoom;
    private Consumer<RoomRequest> onJoinRoom;

    public MainView() {
        setSpacing(12);
        setPadding(new Insets(16));

        var title = new Label("RLan");
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");

        var createSection = buildCreateSection();
        var joinSection = buildJoinSection();

        getChildren().addAll(title, createSection, joinSection, statusLabel);
    }

    private VBox buildCreateSection() {
        var heading = new Label("创建房间");
        heading.setStyle("-fx-font-weight: bold;");

        var grid = new GridPane();
        grid.setHgap(8);
        grid.setVgap(8);
        grid.add(new Label("房间名称:"), 0, 0);
        grid.add(roomNameField, 1, 0);
        grid.add(new Label("房间密码:"), 0, 1);
        grid.add(createPasswordField, 1, 1);

        var btn = new Button("创建房间");
        btn.setOnAction(e -> {
            if (onCreateRoom != null) {
                onCreateRoom.accept(new RoomRequest(
                        roomNameField.getText().trim(),
                        createPasswordField.getText(),
                        null));
            }
        });

        var box = new VBox(8, heading, grid, btn);
        box.setStyle("-fx-border-color: #ccc; -fx-border-radius: 6; -fx-padding: 10;");
        return box;
    }

    private VBox buildJoinSection() {
        var heading = new Label("加入房间");
        heading.setStyle("-fx-font-weight: bold;");

        var grid = new GridPane();
        grid.setHgap(8);
        grid.setVgap(8);
        grid.add(new Label("房间 ID:"), 0, 0);
        grid.add(joinIdField, 1, 0);
        grid.add(new Label("房间密码:"), 0, 1);
        grid.add(joinPasswordField, 1, 1);

        var btn = new Button("加入房间");
        btn.setOnAction(e -> {
            if (onJoinRoom != null) {
                UUID id = null;
                try {
                    id = UUID.fromString(joinIdField.getText().trim());
                } catch (IllegalArgumentException ex) {
                    statusLabel.setText("房间 ID 格式无效");
                    return;
                }
                onJoinRoom.accept(new RoomRequest(
                        null,
                        joinPasswordField.getText(),
                        id));
            }
        });

        var box = new VBox(8, heading, grid, btn);
        box.setStyle("-fx-border-color: #ccc; -fx-border-radius: 6; -fx-padding: 10;");
        return box;
    }

    public void status(String text) {
        statusLabel.setText(text);
    }

    public void onCreateRoom(Consumer<RoomRequest> handler) {
        this.onCreateRoom = handler;
    }

    public void onJoinRoom(Consumer<RoomRequest> handler) {
        this.onJoinRoom = handler;
    }

    public static final class RoomRequest {
        public final String name;
        public final String password;
        public final UUID roomId;

        public RoomRequest(String name, String password, UUID roomId) {
            this.name = name;
            this.password = password;
            this.roomId = roomId;
        }
    }
}
