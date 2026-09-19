package rlan.ui;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import rlan.config.Config;
import rlan.net.NetUtil;
import rlan.net.p2p.ConnectionManager;
import rlan.room.RoomHandle;
import rlan.room.RoomManager;
import rlan.room.RoomService;

import java.net.InetSocketAddress;
import java.util.UUID;

public final class App extends Application {
    private static final double WIDTH = 760;
    private static final double HEIGHT = 480;

    private static final InetSocketAddress[] STUN_SERVERS = {
            new InetSocketAddress("stun.l.google.com", 19302),
            new InetSocketAddress("stun1.l.google.com", 19302),
            new InetSocketAddress("stun.qq.com", 3478),
            new InetSocketAddress("stun.miwifi.com", 3478),
            new InetSocketAddress("stun.syncthing.net", 3478)
    };

    @Override
    public void start(Stage stage) {
        var roomManager = new RoomManager();
        var selfId = UUID.randomUUID().toString().substring(0, 8);
        var config = new Config();
        var connectionManager = new ConnectionManager(config);
        var roomService = new RoomService(roomManager, connectionManager, selfId);

        try {
            connectionManager.start(roomService.messageHandler());
        } catch (InterruptedException e) {
            new Alert(Alert.AlertType.ERROR, "网络层启动失败: " + e.getMessage()).showAndWait();
            return;
        }

        var localPort = connectionManager.localAddress().getPort();
        var lanHost = NetUtil.findLanAddress().getHostAddress();

        String publicHost = null;
        int publicPort = 0;
        for (var stun : STUN_SERVERS) {
            try {
                var publicAddr = connectionManager.discoverPublicAddress(stun);
                publicHost = publicAddr.getHostString();
                publicPort = publicAddr.getPort();
                break;
            } catch (Exception ignored) {
            }
        }
        boolean hasPublicAddress = publicHost != null;
        final String finalPublicHost = publicHost;
        final int finalPublicPort = publicPort;

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
                    entry.name, entry.id, entry.handle, "待分配", 1, 8, isOwner);
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
            var room = roomService.createRoom(req.name, req.password);
            String handleHost = hasPublicAddress ? finalPublicHost : lanHost;
            int handlePort = hasPublicAddress ? finalPublicPort : localPort;
            var handle = new RoomHandle(room.id(), handleHost, handlePort);
            session.addRoom(room.id(), req.name, RoomSession.Role.OWNER, handle.encode());
            mainView.clearCreateFields();
            mainView.status("已创建房间: " + req.name + (hasPublicAddress ? " (公网可达)" : " (仅局域网可达)"));
            var alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("房间创建成功");
            alert.setHeaderText("房间「" + req.name + "」已创建");
            alert.setContentText("房间 ID:\n" + handle.encode() + "\n\n请将此 ID 与房间密码告知其他参与者。");
            alert.showAndWait();
        });

        mainView.onJoinRoom(req -> {
            RoomHandle handle;
            try {
                handle = RoomHandle.decode(req.roomId);
            } catch (IllegalArgumentException e) {
                mainView.status("房间 ID 格式无效: " + e.getMessage());
                return;
            }
            mainView.status("正在查询房间...");
            roomService.joinRoomRemote(handle, req.password, result -> Platform.runLater(() -> {
                if (result.success) {
                    session.addRoom(handle.id(), result.roomName, RoomSession.Role.MEMBER, handle.encode());
                    mainView.clearJoinFields();
                    mainView.status("已加入房间: " + result.roomName);
                } else {
                    mainView.status("加入失败: " + result.error);
                }
            }));
        });

        var scene = new Scene(root, WIDTH, HEIGHT);
        scene.getStylesheets().add(getClass().getResource("/rlan.css").toExternalForm());
        stage.setTitle("RLan - 远程局域网联机工具");
        stage.setScene(scene);
        stage.setMinWidth(WIDTH);
        stage.setMinHeight(HEIGHT);
        stage.setOnCloseRequest(e -> connectionManager.close());
        stage.show();
    }
}
