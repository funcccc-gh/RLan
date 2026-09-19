package rlan.ui;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.UUID;

public final class RoomView extends VBox {
    private final Label roomNameLabel = new Label();
    private final Label roomIdLabel = new Label();
    private final Label roleLabel = new Label();
    private final Label createdAtLabel = new Label();
    private final Label capacityLabel = new Label();
    private final Label lanIpLabel = new Label();
    private final Label localPortLabel = new Label();
    private final Label publicAddrLabel = new Label();
    private final Label natTypeLabel = new Label();
    private final Label reachabilityLabel = new Label();
    private final ObservableList<String> memberItems = FXCollections.observableArrayList();
    private final ListView<String> memberList = new ListView<>(memberItems);
    private final Button actionButton = new Button();
    private final Button backButton = new Button("← 返回");
    private final Button copyIdButton = new Button("复制 ID");
    private final VBox passwordSection = new VBox(6);
    private final TextField newPasswordField = new TextField();
    private final Button changePasswordButton = new Button("修改密码");
    private UUID currentRoomId;
    private String currentHandle;

    public RoomView() {
        setSpacing(10);
        setPadding(new Insets(16));

        var heading = new Label("房间详情");
        heading.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        var idBox = new HBox(8, roomIdLabel, copyIdButton);
        var infoGrid = new GridPane();
        infoGrid.setHgap(10);
        infoGrid.setVgap(4);
        infoGrid.add(new Label("房间名称:"), 0, 0);
        infoGrid.add(roomNameLabel, 1, 0);
        infoGrid.add(new Label("我的角色:"), 0, 1);
        infoGrid.add(roleLabel, 1, 1);
        infoGrid.add(new Label("创建时间:"), 0, 2);
        infoGrid.add(createdAtLabel, 1, 2);
        infoGrid.add(new Label("容量:"), 0, 3);
        infoGrid.add(capacityLabel, 1, 3);

        var infoBox = new VBox(4, idBox, infoGrid);
        infoBox.setStyle("-fx-border-color: #ccc; -fx-border-radius: 6; -fx-padding: 10;");

        var netHeading = new Label("网络信息");
        netHeading.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
        var netGrid = new GridPane();
        netGrid.setHgap(10);
        netGrid.setVgap(4);
        netGrid.add(new Label("局域网 IP:"), 0, 0);
        netGrid.add(lanIpLabel, 1, 0);
        netGrid.add(new Label("本地端口:"), 0, 1);
        netGrid.add(localPortLabel, 1, 1);
        netGrid.add(new Label("公网地址:"), 0, 2);
        netGrid.add(publicAddrLabel, 1, 2);
        netGrid.add(new Label("NAT 类型:"), 0, 3);
        netGrid.add(natTypeLabel, 1, 3);
        netGrid.add(new Label("可达性:"), 0, 4);
        netGrid.add(reachabilityLabel, 1, 4);

        var netBox = new VBox(6, netHeading, netGrid);
        netBox.setStyle("-fx-border-color: #ccc; -fx-border-radius: 6; -fx-padding: 10;");

        memberList.setPrefHeight(140);
        var membersHeading = new Label("成员列表");
        membersHeading.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

        var pwHeading = new Label("修改房间密码");
        pwHeading.setStyle("-fx-font-weight: bold;");
        newPasswordField.setPromptText("输入新密码");
        var pwBox = new HBox(8, newPasswordField, changePasswordButton);
        passwordSection.getChildren().addAll(pwHeading, pwBox);
        passwordSection.setStyle("-fx-border-color: #ccc; -fx-border-radius: 6; -fx-padding: 8;");
        passwordSection.setVisible(false);
        passwordSection.setManaged(false);

        var bottomBox = new HBox(8, backButton, actionButton);

        getChildren().addAll(heading, infoBox, netBox, membersHeading, memberList, passwordSection, bottomBox);
    }

    public void setRoomInfo(String name, UUID id, String handle, String selfVip, int memberCount, int maxDevices, boolean isOwner) {
        this.currentRoomId = id;
        this.currentHandle = handle;
        roomNameLabel.setText(name);
        roomIdLabel.setText("房间 ID: " + handle);
        roleLabel.setText(isOwner ? "房主 👑" : "成员");
        capacityLabel.setText(memberCount + " / " + maxDevices + " 台设备");
        actionButton.setText(isOwner ? "关闭房间" : "离开房间");
        passwordSection.setVisible(isOwner);
        passwordSection.setManaged(isOwner);
        if (!isOwner) {
            newPasswordField.clear();
        }
    }

    public void setCreatedAt(long timestamp) {
        var fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());
        createdAtLabel.setText(fmt.format(Instant.ofEpochMilli(timestamp)));
    }

    public void setNetworkDetails(String lanIp, int localPort, String publicAddr, String natType, boolean hasPublicAddress) {
        lanIpLabel.setText(lanIp != null ? lanIp : "未检测");
        localPortLabel.setText(localPort > 0 ? String.valueOf(localPort) : "未启动");
        publicAddrLabel.setText(publicAddr != null ? publicAddr : "无");
        natTypeLabel.setText(natType != null ? natType : "未知");
        if (hasPublicAddress) {
            reachabilityLabel.setText("✅ 公网可达（跨局域网可加入）");
            reachabilityLabel.setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");
        } else {
            reachabilityLabel.setText("⚠ 仅局域网可达");
            reachabilityLabel.setStyle("-fx-text-fill: #e67e22; -fx-font-weight: bold;");
        }
    }

    public void setMembers(Collection<String> members, String ownerId) {
        memberItems.setAll(members.stream()
                .map(m -> m.equals(ownerId) ? "👑 " + m + " (房主)" : m)
                .toList());
    }

    public void onAction(Runnable handler) {
        actionButton.setOnAction(e -> handler.run());
    }

    public void onBack(Runnable handler) {
        backButton.setOnAction(e -> handler.run());
    }

    public void onChangePassword(java.util.function.Consumer<String> handler) {
        changePasswordButton.setOnAction(e -> {
            var newPw = newPasswordField.getText();
            if (newPw.isEmpty()) {
                return;
            }
            handler.accept(newPw);
            newPasswordField.clear();
        });
    }

    public void setupCopyIdButton() {
        copyIdButton.setOnAction(e -> {
            if (currentHandle == null) {
                return;
            }
            var clipboard = Clipboard.getSystemClipboard();
            var content = new ClipboardContent();
            content.putString(currentHandle);
            clipboard.setContent(content);
            copyIdButton.setText("已复制 ✓");
            javafx.animation.PauseTransition pt = new javafx.animation.PauseTransition(javafx.util.Duration.seconds(1.5));
            pt.setOnFinished(ev -> copyIdButton.setText("复制 ID"));
            pt.play();
        });
    }
}
