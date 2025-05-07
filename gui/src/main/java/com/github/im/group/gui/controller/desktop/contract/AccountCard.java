package com.github.im.group.gui.controller.desktop.contract;

import com.github.im.dto.user.FriendshipDTO;
import com.github.im.dto.user.UserInfo;
import com.github.im.group.gui.util.AvatarGenerator;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Text;
import lombok.Getter;

/**
 * AccountCard 用于展示用户简略信息，如头像和姓名
 */
public class AccountCard extends StackPane {

    private ImageView avatar;
    private Text nameText;

    private Label nameLabel;
    private HoverCard hoverCard;

    @Getter
    private UserInfo userInfo;

    public AccountCard(UserInfo item) {

        userInfo = item;

        // 更新ListTile的主要内容（头像和用户名）
        nameLabel = new Label(userInfo.getUsername());

        var image = AvatarGenerator.generateSquareAvatarWithRoundedCorners(userInfo.getUsername(), 30);
        avatar= new ImageView(image);


        // 创建 HoverCard
        hoverCard = new HoverCard(userInfo.getUsername(), userInfo.getUsername(), image);

        // 设置布局
//        HBox summaryBox = new HBox(10, avatar, nameText);
//        summaryBox.setStyle("-fx-background-color: #f4f4f4; -fx-padding: 10px; -fx-border-radius: 5px;");
//        summaryBox.setOnMouseEntered(e -> hoverCard.showDetails());
//        summaryBox.setOnMouseExited(e -> hoverCard.hideDetails());

        // 将组件加入主 StackPane
//        getChildren().addAll(summaryBox, hoverCard);

        this.setOnMouseEntered(e -> hoverCard.showDetails());
        this.setOnMouseExited(e -> hoverCard.hideDetails());

        this.getChildren().addAll(nameLabel,avatar);

    }


}
