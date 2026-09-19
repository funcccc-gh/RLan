package rlan.ui;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.Collection;
import java.util.UUID;

public final class RoomView extends VBox {
    private final Label roomNameLabel = new Label();
    private final Label roomIdLabel = new Label();
    private final Label selfVipLabel = new Label();
    private final Label capacityLabel = new Label();
    private final ObservableList<String> memberItems = FXCollections.observableArrayList();
    private final ListView<String> memberList = new ListView<>(memberItems);
    private final TextField messageField = new TextField();

    public RoomView() {
        setSpacing(10);
        setPadding(new Insets(16));

        var heading = new Label("房间");
        heading.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        var infoBox = new VBox(4,
                roomNameLabel,
                roomIdLabel,
                selfVipLabel,
                capacityLabel);

        memberList.setPrefHeight(160);
        var membersHeading = new Label("成员列表:");
        membersHeading.setStyle("-fx-font-weight: bold;");

        var sendBox = new HBox(8, messageField);
        messageField.setPromptText("输入消息（回车发送）");
        HBox.setHgrow(messageField, javafx.scene.layout.Priority.ALWAYS);

        var leaveBtn = new Button("离开房间");

        getChildren().addAll(heading, infoBox, membersHeading, memberList, sendBox, leaveBtn);
    }

    public void setRoomInfo(String name, UUID id, String selfVip, int memberCount, int maxDevices) {
        roomNameLabel.setText("房间名称: " + name);
        roomIdLabel.setText("房间 ID: " + id);
        selfVipLabel.setText("本机虚拟 IP: " + selfVip);
        capacityLabel.setText("容量: " + memberCount + " / " + maxDevices);
    }

    public void setMembers(Collection<String> members) {
        memberItems.setAll(members);
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

    public void onLeave(Runnable handler) {
        var leaveBtn = (Button) getChildren().get(getChildren().size() - 1);
        leaveBtn.setOnAction(e -> handler.run());
    }
}
