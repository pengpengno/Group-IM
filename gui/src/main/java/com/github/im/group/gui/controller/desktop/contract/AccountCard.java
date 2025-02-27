package com.github.im.group.gui.controller.desktop.contract;

import com.github.im.dto.user.FriendshipDTO;
import com.github.im.dto.user.UserInfo;
import com.github.im.group.gui.util.AvatarGenerator;
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

    public AccountCard(FriendshipDTO item) {

//        currentItem = item;
//
//        if (empty || item == null) {
//            setGraphic(null);
//        } else {
            UserInfo userInfo = item.getFriendUserInfo();

            // 更新ListTile的主要内容（头像和用户名）
            nameText = new Text();
            nameText.textProperty().set(userInfo.getUsername());
            var image = AvatarGenerator.generateCircleAvatar(userInfo.getUsername(), 50);
            avatar= new ImageView(image);
//            tile.setPrimaryGraphic(imageView);

            // 添加更多信息或交互按钮（如编辑、删除等）
//            buttonBar.getChildren().clear();
            // 这里可以添加按钮进行操作，例如编辑、删除等
//             buttonBar.getChildren().add(MaterialDesignIcon.EDIT.button(e -> editItem(item)));

            // 设置当前图形内容
//            setGraphic(tile);
//        }

        this.getChildren().addAll(nameText,avatar);

    }



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
