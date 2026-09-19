package rlan.ui;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import rlan.config.Config;
import rlan.config.UserSettings;
import rlan.net.NetUtil;
import rlan.net.p2p.ConnectionManager;
import rlan.room.RoomHandle;
import rlan.room.RoomManager;
import rlan.room.RoomService;
import rlan.room.RoomStore;

import java.net.InetSocketAddress;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class App extends Application {
    private static final double WIDTH = 820;
    private static final double HEIGHT = 520;
    private static final boolean PERSISTENCE_ENABLED = true;
    private static java.awt.TrayIcon trayIcon;
    private static Stage primaryStage;

    private static final InetSocketAddress[] STUN_SERVERS = {
            new InetSocketAddress("stun.l.google.com", 19302),
            new InetSocketAddress("stun1.l.google.com", 19302),
            new InetSocketAddress("stun.qq.com", 3478),
            new InetSocketAddress("stun.miwifi.com", 3478),
            new InetSocketAddress("stun.syncthing.net", 3478)
    };

    @Override
    public void start(Stage stage) {
        var userSettings = new UserSettings();
        var roomManager = new RoomManager();
        var selfId = userSettings.nickname();
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
        final String finalLanHost = lanHost;
        final int finalLocalPort = localPort;

        var session = new RoomSession();
        var roomStore = new RoomStore();
        ConcurrentMap<UUID, String> roomPasswords = new ConcurrentHashMap<>();
        Runnable saveRooms = () -> {
            var records = new java.util.ArrayList<RoomStore.RoomRecord>();
            for (var entry : session.rooms()) {
                var pw = roomPasswords.get(entry.id);
                if (pw != null) {
                    records.add(new RoomStore.RoomRecord(entry.name, entry.role.name(), entry.handle, pw));
                }
            }
            roomStore.save(records);
        };
        var tabBar = new TabBarView();
        var mainView = new MainView();
        var roomListView = new RoomListView(session);
        var roomView = new RoomView();
        var chatView = new ChatView();
        var settingsView = new SettingsView();

        settingsView.setNickname(userSettings.nickname());
        settingsView.setTrayEnabled(userSettings.trayEnabled());
        settingsView.setNetworkInfo(
                lanHost,
                localPort,
                hasPublicAddress ? finalPublicHost + ":" + finalPublicPort : null,
                hasPublicAddress ? "锥型 NAT (公网可达)" : "对称型/未知"
        );

        var settingsScroll = new javafx.scene.control.ScrollPane(settingsView);
        settingsScroll.setFitToWidth(true);
        settingsScroll.setFitToHeight(true);

        var content = new BorderPane();
        content.setCenter(mainView);

        var root = new BorderPane();
        root.setLeft(tabBar);
        root.setCenter(content);

        tabBar.onTabSelect(tab -> {
            switch (tab) {
                case CREATE_JOIN -> content.setCenter(mainView);
                case ROOM_LIST -> content.setCenter(roomListView);
                case CHAT -> content.setCenter(chatView);
                case SETTINGS -> content.setCenter(settingsScroll);
            }
        });

        roomListView.onSelect(entry -> {
            session.select(entry);
            chatView.setRoomName(entry.name);
            boolean isOwner = entry.role == RoomSession.Role.OWNER;
            roomView.setRoomInfo(
                    entry.name, entry.id, entry.handle,
                    hasPublicAddress ? finalPublicHost + ":" + finalPublicPort : finalLanHost + ":" + finalLocalPort,
                    1, 8, isOwner);
            roomView.setNetworkDetails(
                    finalLanHost, finalLocalPort,
                    hasPublicAddress ? finalPublicHost + ":" + finalPublicPort : null,
                    hasPublicAddress ? "锥型 NAT" : "对称型/未知",
                    hasPublicAddress);
            var room = roomManager.find(entry.id).orElse(null);
            if (room != null) {
                roomView.setCreatedAt(room.createdAt());
            } else {
                roomView.setCreatedAt(System.currentTimeMillis());
            }
            roomView.setMembers(java.util.List.of(entry.name), entry.name);
            roomView.setupCopyIdButton();
            roomView.onAction(() -> {
                session.removeRoom(entry.id);
                roomPasswords.remove(entry.id);
                saveRooms.run();
                content.setCenter(roomListView);
                roomListView.clearSelection();
                mainView.status("已" + (isOwner ? "关闭" : "离开") + "房间: " + entry.name);
            });
            roomView.onBack(() -> {
                content.setCenter(roomListView);
                roomListView.clearSelection();
            });
            roomView.onChangePassword(newPw -> {
                if (isOwner) {
                    String handleHost = hasPublicAddress ? finalPublicHost : finalLanHost;
                    int handlePort = hasPublicAddress ? finalPublicPort : finalLocalPort;
                    var newHandle = roomService.migrateRoom(entry.id, newPw, handleHost, handlePort);
                    if (newHandle != null) {
                        var encoded = newHandle.encode(newPw);
                        session.removeRoom(entry.id);
                        roomPasswords.remove(entry.id);
                        session.addRoom(newHandle.id(), entry.name, RoomSession.Role.OWNER, encoded);
                        roomPasswords.put(newHandle.id(), newPw);
                        saveRooms.run();
                        mainView.status("房间「" + entry.name + "」已迁移到新房间（密码已修改）");
                        content.setCenter(roomListView);
                    } else {
                        mainView.status("密码修改失败: 房间不存在");
                    }
                }
            });
            var roomScroll = new javafx.scene.control.ScrollPane(roomView);
            roomScroll.setFitToWidth(true);
            roomScroll.setFitToHeight(true);
            content.setCenter(roomScroll);
        });

        mainView.onCreateRoom(req -> {
            if (req.name.isEmpty()) {
                mainView.status("房间名称不能为空");
                return;
            }
            var room = roomService.createRoom(req.name, req.password);
            String handleHost = hasPublicAddress ? finalPublicHost : finalLanHost;
            int handlePort = hasPublicAddress ? finalPublicPort : finalLocalPort;
            var handle = new RoomHandle(room.id(), handleHost, handlePort);
            var encodedHandle = handle.encode(req.password);
            session.addRoom(room.id(), req.name, RoomSession.Role.OWNER, encodedHandle);
            roomPasswords.put(room.id(), req.password);
            saveRooms.run();
            mainView.clearCreateFields();
            mainView.status("已创建房间: " + req.name + (hasPublicAddress ? " (公网可达)" : " (仅局域网可达)"));
            var alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("房间创建成功");
            alert.setHeaderText("房间「" + req.name + "」已创建");
            alert.setContentText("房间 ID (已加密):\n" + encodedHandle + "\n\n请将此 ID 与房间密码告知其他参与者。");
            alert.showAndWait();
            tabBar.select(TabBarView.Tab.ROOM_LIST);
            content.setCenter(roomListView);
        });

        mainView.onJoinRoom(req -> {
            RoomHandle handle;
            try {
                handle = RoomHandle.decode(req.roomId, req.password);
            } catch (IllegalArgumentException e) {
                mainView.status("房间 ID 或密码无效: " + e.getMessage());
                return;
            } catch (RuntimeException e) {
                mainView.status("解密失败: 房间密码错误或房间 ID 无效");
                return;
            }
            mainView.status("正在查询房间...");
            roomService.joinRoomRemote(handle, req.password, result -> Platform.runLater(() -> {
                if (result.success) {
                    session.addRoom(handle.id(), result.roomName, RoomSession.Role.MEMBER, req.roomId);
                    roomPasswords.put(handle.id(), req.password);
                    saveRooms.run();
                    mainView.clearJoinFields();
                    mainView.status("已加入房间: " + result.roomName);
                    tabBar.select(TabBarView.Tab.ROOM_LIST);
                    content.setCenter(roomListView);
                } else {
                    mainView.status("加入失败: " + result.error);
                }
            }));
        });

        roomService.onChatMessage(proto -> Platform.runLater(() -> {
            var msg = proto;
            switch (msg.kind()) {
                case TEXT -> chatView.addMessage(new ChatView.ChatMessage(
                        msg.senderId(), msg.text(), msg.timestamp()));
                case IMAGE -> chatView.addMessage(new ChatView.ChatMessage(
                        msg.senderId(), msg.text(), msg.timestamp(), true, msg.text(), msg.fileData()));
                case FILE -> chatView.addMessage(new ChatView.ChatMessage(
                        msg.senderId(), msg.text(), msg.timestamp(), false, msg.text(), msg.fileData()));
            }
        }));

        roomService.onRoomMigrated(migration -> Platform.runLater(() -> {
            try {
                var newHandle = RoomHandle.decode(migration.newEncodedHandle, migration.newPassword);
                mainView.status("房间已迁移，正在加入新房间...");
                roomService.joinRoomRemote(newHandle, migration.newPassword, result -> Platform.runLater(() -> {
                    if (result.success) {
                        session.addRoom(newHandle.id(), result.roomName, RoomSession.Role.MEMBER, migration.newEncodedHandle);
                        roomPasswords.put(newHandle.id(), migration.newPassword);
                        saveRooms.run();
                        mainView.status("已自动加入迁移后的房间: " + result.roomName);
                        tabBar.select(TabBarView.Tab.ROOM_LIST);
                        content.setCenter(roomListView);
                    } else {
                        mainView.status("自动加入新房间失败: " + result.error);
                    }
                }));
            } catch (Exception e) {
                mainView.status("房间迁移通知解析失败: " + e.getMessage());
            }
        }));

        chatView.onSendMessage(text -> {
            var selected = session.selected();
            if (selected == null) {
                chatView.setRoomName("请先在房间列表中选择一个房间");
                return;
            }
            roomService.sendChatText(selected.id, text);
            chatView.addMessage(new ChatView.ChatMessage(selfId, text, System.currentTimeMillis()));
        });

        chatView.onSendFile((fileName, data) -> {
            var selected = session.selected();
            if (selected == null) {
                chatView.setRoomName("请先在房间列表中选择一个房间");
                return;
            }
            boolean isImage = fileName.toLowerCase().matches(".*\\.(png|jpg|jpeg|gif|bmp)$");
            if (isImage) {
                roomService.sendChatImage(selected.id, fileName, data);
                chatView.addMessage(new ChatView.ChatMessage(selfId, fileName, System.currentTimeMillis(), true, fileName, data));
            } else {
                roomService.sendChatFile(selected.id, fileName, data);
                chatView.addMessage(new ChatView.ChatMessage(selfId, fileName, System.currentTimeMillis(), false, fileName, data));
            }
        });

        settingsView.onStackChange(stack -> {
            settingsView.setNetworkInfo(
                    finalLanHost,
                    finalLocalPort,
                    hasPublicAddress ? finalPublicHost + ":" + finalPublicPort : null,
                    hasPublicAddress ? "锥型 NAT (公网可达)" : "对称型/未知"
            );
            mainView.status("IP 协议栈已切换: " + stack);
        });

        settingsView.onNicknameChange(name -> {
            userSettings.nickname(name);
            userSettings.save();
            settingsView.setNickname(name);
            mainView.status("昵称已更新: " + name);
        });

        settingsView.onTrayToggle(enabled -> {
            userSettings.trayEnabled(enabled);
            userSettings.save();
            if (enabled) {
                new Thread(() -> setupTrayIcon(stage, connectionManager)).start();
            } else {
                removeTrayIcon();
            }
            mainView.status(enabled ? "已启用最小化到托盘" : "已禁用最小化到托盘");
        });

        if (userSettings.trayEnabled()) {
            new Thread(() -> setupTrayIcon(stage, connectionManager)).start();
        }

        if (PERSISTENCE_ENABLED && roomStore.exists()) {
            var records = roomStore.load();
            for (var record : records) {
                try {
                    var handle = RoomHandle.decode(record.encodedHandle, record.password);
                    if ("OWNER".equals(record.role)) {
                        var room = roomService.createRoom(record.name, record.password);
                        session.addRoom(room.id(), record.name, RoomSession.Role.OWNER, record.encodedHandle);
                        roomPasswords.put(room.id(), record.password);
                    } else {
                        roomService.joinRoomRemote(handle, record.password, result -> Platform.runLater(() -> {
                            if (result.success) {
                                session.addRoom(handle.id(), result.roomName, RoomSession.Role.MEMBER, record.encodedHandle);
                                roomPasswords.put(handle.id(), record.password);
                                saveRooms.run();
                            }
                        }));
                    }
                } catch (Exception ignored) {
                }
            }
        }

        var scene = new Scene(root, WIDTH, HEIGHT);
        scene.getStylesheets().add(getClass().getResource("/rlan.css").toExternalForm());
        stage.setTitle("RLan - 远程局域网联机工具");
        stage.setScene(scene);
        stage.setMinWidth(WIDTH);
        stage.setMinHeight(HEIGHT);
        primaryStage = stage;
        stage.setOnCloseRequest(e -> {
            if (userSettings.trayEnabled() && trayIcon != null) {
                e.consume();
                stage.hide();
            } else {
                connectionManager.close();
            }
        });
        stage.show();
    }

    private static void setupTrayIcon(Stage stage, ConnectionManager connectionManager) {
        if (!java.awt.SystemTray.isSupported()) {
            return;
        }
        removeTrayIcon();
        var tray = java.awt.SystemTray.getSystemTray();
        var image = createTrayImage();
        var popup = new java.awt.PopupMenu();
        var showItem = new java.awt.MenuItem("Show");
        showItem.addActionListener(e -> Platform.runLater(() -> {
            stage.show();
            stage.setIconified(false);
            stage.toFront();
            stage.requestFocus();
        }));
        var exitItem = new java.awt.MenuItem("Exit");
        exitItem.addActionListener(e -> Platform.runLater(() -> {
            removeTrayIcon();
            connectionManager.close();
            stage.close();
            Platform.exit();
        }));
        popup.add(showItem);
        popup.addSeparator();
        popup.add(exitItem);

        trayIcon = new java.awt.TrayIcon(image, "RLan", popup);
        trayIcon.setImageAutoSize(true);
        trayIcon.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mousePressed(java.awt.event.MouseEvent e) {
                Platform.runLater(() -> {
                    stage.show();
                    stage.setIconified(false);
                    stage.toFront();
                    stage.requestFocus();
                });
            }
        });
        try {
            tray.add(trayIcon);
        } catch (java.awt.AWTException ignored) {
        }
    }

    private static void removeTrayIcon() {
        if (trayIcon != null) {
            java.awt.SystemTray.getSystemTray().remove(trayIcon);
            trayIcon = null;
        }
    }

    private static java.awt.Image createTrayImage() {
        var img = new java.awt.image.BufferedImage(16, 16, java.awt.image.BufferedImage.TYPE_INT_ARGB);
        var g = img.getGraphics();
        g.setColor(new java.awt.Color(74, 144, 217));
        g.fillRect(0, 0, 16, 16);
        g.setColor(java.awt.Color.WHITE);
        g.setFont(new java.awt.Font("SansSerif", java.awt.Font.BOLD, 11));
        g.drawString("R", 4, 12);
        g.dispose();
        return img;
    }
}
