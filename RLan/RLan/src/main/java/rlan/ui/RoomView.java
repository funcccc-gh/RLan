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
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.Collection;
import java.util.UUID;

public final class RoomView extends VBox {
    private final Label roomNameLabel = new Label();
    private final Label roomIdLabel = new Label();
    private final Label selfVipLabel = new Label();
    private final Label capacityLabel = new Label();
    private final Label roleLabel = new Label();
    private final ObservableList<String> memberItems = FXCollections.observableArrayList();
    private final ListView<String> memberList = new ListView<>(memberItems);
    private final TextField messageField = new TextField();
    private final Button actionButton = new Button();
    private final Button backButton = new Button("← 返回主页");
    private final Button copyIdButton = new Button("复制 ID");
    private UUID currentRoomId;

    public RoomView() {
        setSpacing(10);
        setPadding(new Insets(16));

        var heading = new Label("房间详情");
        heading.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        var idBox = new HBox(8, roomIdLabel, copyIdButton);
        var infoBox = new VBox(4, roomNameLabel, idBox, roleLabel, selfVipLabel, capacityLabel);

        memberList.setPrefHeight(180);
        var membersHeading = new Label("成员列表:");
        membersHeading.setStyle("-fx-font-weight: bold;");

        var sendBox = new HBox(8, messageField);
        messageField.setPromptText("输入消息（回车发送）");
        HBox.setHgrow(messageField, javafx.scene.layout.Priority.ALWAYS);

        var bottomBox = new HBox(8, backButton, actionButton);

        getChildren().addAll(heading, infoBox, membersHeading, memberList, sendBox, bottomBox);
    }

    public void setRoomInfo(String name, UUID id, String selfVip, int memberCount, int maxDevices, boolean isOwner) {
        this.currentRoomId = id;
        roomNameLabel.setText("房间名称: " + name);
        roomIdLabel.setText("房间 ID: " + id);
        roleLabel.setText("我的角色: " + (isOwner ? "房主 👑" : "成员"));
        selfVipLabel.setText("本机虚拟 IP: " + selfVip);
        capacityLabel.setText("容量: " + memberCount + " / " + maxDevices);
        actionButton.setText(isOwner ? "关闭房间" : "离开房间");
    }

    public void setMembers(Collection<String> members, String ownerId) {
        memberItems.setAll(members.stream()
                .map(m -> m.equals(ownerId) ? "👑 " + m + " (房主)" : m)
                .toList());
    }

    public void onSend(javafx.event.EventHandler<javafx.scene.input.KeyEvent> handler) {
        messageField.setOnKeyPressed(handler);
    }

    public String messageText() {
        return messageField.getText();
    }

    public void clearMessage() {
        messageField.clear();
    }

    public void onAction(Runnable handler) {
        actionButton.setOnAction(e -> handler.run());
    }

    public void onBack(Runnable handler) {
        backButton.setOnAction(e -> handler.run());
    }

    public void setupCopyIdButton() {
        copyIdButton.setOnAction(e -> {
            if (currentRoomId == null) {
                return;
            }
            var clipboard = Clipboard.getSystemClipboard();
            var content = new ClipboardContent();
            content.putString(currentRoomId.toString());
            clipboard.setContent(content);
            copyIdButton.setText("已复制 ✓");
            javafx.animation.PauseTransition pt = new javafx.animation.PauseTransition(javafx.util.Duration.seconds(1.5));
            pt.setOnFinished(ev -> copyIdButton.setText("复制 ID"));
            pt.play();
        });
    }
}
