package com.github.im.group.gui.controller.desktop.contract;

import com.gluonhq.charm.glisten.control.Avatar;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.paint.Color;

/**
 * HoverCard 用于展示详细的用户信息，如邮箱、状态等
 */
public class HoverCard extends VBox {
//public class HoverCard extends StackPane {

    private HBox detailBox;
    private Label emailLabel;
    private Label statusLabel;
    private Avatar avatar;
    private Text nameText;

    public HoverCard(String name, String email, Avatar avatarImage) {
//    public HoverCard(String name, String email, String status, Image avatarImage) {
        // 创建详细信息框
        detailBox = new HBox(10);
        avatar = avatarImage;

        nameText = new Text(name);
        emailLabel = new Label("Email: " + email);
        statusLabel = new Label("Status: " + "online");
        statusLabel.setTextFill(Color.DARKBLUE);

        detailBox.getChildren().addAll(avatar, nameText, emailLabel, statusLabel);
        detailBox.setStyle("-fx-background-color: #ffffff; -fx-padding: 15px; -fx-border-radius: 5px; -fx-border-color: #cccccc; -fx-border-width: 1px;");
//        detailBox.setVisible(false); // 默认隐藏详细信息

        // 将组件添加到 HoverCard
        getChildren().add(detailBox);
    }


}
