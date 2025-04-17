package com.github.im.group.gui.controller.desktop.contract;

import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Text;
import javafx.scene.paint.Color;

/**
 * HoverCard 用于展示详细的用户信息，如邮箱、状态等
 */
public class HoverCard extends StackPane {

    private HBox detailBox;
    private Label emailLabel;
    private Label statusLabel;
    private ImageView avatar;
    private Text nameText;

    public HoverCard(String name, String email, Image avatarImage) {
//    public HoverCard(String name, String email, String status, Image avatarImage) {
        // 创建详细信息框
        detailBox = new HBox(10);

        avatar = new ImageView(avatarImage != null ? avatarImage : new Image("default-avatar.png"));
        avatar.setFitWidth(60);
        avatar.setFitHeight(60);
        avatar.setPreserveRatio(true);

        nameText = new Text(name);
        emailLabel = new Label("Email: " + email);
        statusLabel = new Label("Status: " + "🟢");
        statusLabel.setTextFill(Color.DARKBLUE);

        detailBox.getChildren().addAll(avatar, nameText, emailLabel, statusLabel);
        detailBox.setStyle("-fx-background-color: #ffffff; -fx-padding: 15px; -fx-border-radius: 5px; -fx-border-color: #cccccc; -fx-border-width: 1px;");
        detailBox.setVisible(false); // 默认隐藏详细信息

        // 将组件添加到 HoverCard
        getChildren().add(detailBox);
    }

    // 显示详细信息
    public void showDetails() {
        detailBox.setVisible(true);
    }

    // 隐藏详细信息
    public void hideDetails() {
        detailBox.setVisible(false);
    }
}
