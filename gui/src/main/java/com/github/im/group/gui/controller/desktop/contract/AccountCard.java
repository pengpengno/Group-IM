package com.github.im.group.gui.controller.desktop.contract;

import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Text;

/**
 * AccountCard 用于展示用户简略信息，如头像和姓名
 */
public class AccountCard extends StackPane {

    private String name;
    private ImageView avatar;
    private Text nameText;
    private HoverCard hoverCard;

    public AccountCard(String name, String email, Image avatarImage) {
        this.name = name;

        // 创建头像
        avatar = new ImageView(avatarImage != null ? avatarImage : new Image("default-avatar.png"));
        avatar.setFitWidth(50);
        avatar.setFitHeight(50);
        avatar.setPreserveRatio(true);

        // 创建姓名文本
        nameText = new Text(name);

        // 创建 HoverCard
        hoverCard = new HoverCard(name, email, avatarImage);

        // 设置布局
        HBox summaryBox = new HBox(10, avatar, nameText);
        summaryBox.setStyle("-fx-background-color: #f4f4f4; -fx-padding: 10px; -fx-border-radius: 5px;");
        summaryBox.setOnMouseEntered(e -> hoverCard.showDetails());
        summaryBox.setOnMouseExited(e -> hoverCard.hideDetails());

        // 将组件加入主 StackPane
        getChildren().addAll(summaryBox, hoverCard);
    }
}
