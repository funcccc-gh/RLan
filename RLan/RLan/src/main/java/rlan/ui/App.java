package rlan.ui;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

public final class App extends Application {
    private static final double WIDTH = 760;
    private static final double HEIGHT = 480;

    @Override
    public void start(Stage stage) {
        var roomManager = new rlan.room.RoomManager();
        var selfId = "self";
        var session = new RoomSession();
        var sidebar = new SidebarView(session);
        var mainView = new MainView();
        var roomView = new RoomView();

        var content = new BorderPane();
        content.setCenter(mainView);

        var root = new BorderPane();
        root.setLeft(sidebar);
        root.setCenter(content);

        sidebar.onSelect(entry -> {
            session.select(entry);
            boolean isOwner = entry.role == RoomSession.Role.OWNER;
            roomView.setRoomInfo(
                    entry.name, entry.id, "待分配", 1, 8, isOwner);
            roomView.setMembers(java.util.List.of(entry.name), entry.name);
            roomView.setupCopyIdButton();
            roomView.onAction(() -> {
                session.removeRoom(entry.id);
                content.setCenter(mainView);
                sidebar.clearSelection();
                mainView.status("已" + (isOwner ? "关闭" : "离开") + "房间: " + entry.name);
            });
            roomView.onBack(() -> {
                content.setCenter(mainView);
                sidebar.clearSelection();
            });
            roomView.onChangePassword(newPw -> {
                if (isOwner) {
                    boolean ok = roomManager.changePassword(entry.id, selfId, newPw);
                    if (ok) {
                        mainView.status("房间「" + entry.name + "」密码已修改");
                    } else {
                        mainView.status("密码修改失败");
                    }
                }
            });
            content.setCenter(roomView);
        });

        mainView.onCreateRoom(req -> {
            if (req.name.isEmpty()) {
                mainView.status("房间名称不能为空");
                return;
            }
            var room = roomManager.create(req.name, req.password, selfId);
            session.addRoom(room.id(), req.name, RoomSession.Role.OWNER);
            mainView.clearCreateFields();
            mainView.status("已创建房间: " + req.name);
            var alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("房间创建成功");
            alert.setHeaderText("房间「" + req.name + "」已创建");
            alert.setContentText("房间 ID:\n" + room.id() + "\n\n请将此 ID 与房间密码告知其他参与者。");
            alert.showAndWait();
        });

        mainView.onJoinRoom(req -> {
            var result = roomManager.join(req.roomId, req.password, selfId);
            if (result == rlan.room.JoinResult.SUCCESS || result == rlan.room.JoinResult.ALREADY_MEMBER) {
                var room = roomManager.find(req.roomId).orElseThrow();
                session.addRoom(req.roomId, room.name(), RoomSession.Role.MEMBER);
                mainView.clearJoinFields();
                mainView.status("已加入房间: " + room.name());
            } else {
                mainView.status("加入失败: " + result);
            }
        });

        var scene = new Scene(root, WIDTH, HEIGHT);
        scene.getStylesheets().add(getClass().getResource("/rlan.css").toExternalForm());
        stage.setTitle("RLan - 远程局域网联机工具");
        stage.setScene(scene);
        stage.setMinWidth(WIDTH);
        stage.setMinHeight(HEIGHT);
        stage.show();
    }
}
