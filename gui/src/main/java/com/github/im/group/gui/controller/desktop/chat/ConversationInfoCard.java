package com.github.im.group.gui.controller.desktop.chat;

import com.github.im.conversation.ConversationRes;
import com.github.im.dto.user.FriendshipDTO;
import com.github.im.dto.user.UserInfo;
import com.github.im.group.gui.controller.desktop.contract.HoverCard;
import com.github.im.group.gui.util.AvatarGenerator;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Text;
import lombok.Getter;
import org.springframework.util.CollectionUtils;

import java.util.List;

/**
 *  用于展示会话简略信息，如头像和姓名
 */
public class ConversationInfoCard extends StackPane {

    private ImageView avatar;
    private Text nameText;
    private HoverCard hoverCard;

    @Getter
    private ConversationRes conversation;

    public ConversationInfoCard(ConversationRes item , String groupName) {

        conversation = item;

        // 更新ListTile的主要内容（头像和用户名）
        nameText = new Text(groupName);

        this.getChildren().addAll(nameText);

    }


    public ConversationInfoCard(ConversationRes item) {

        conversation = item;

        // 更新ListTile的主要内容（头像和用户名）
        nameText = new Text();
        var members = item.getMembers();
        if (CollectionUtils.isEmpty(members)){

        }

//        nameText.textProperty().set(userInfo.getUsername());
//        var image = AvatarGenerator.generateSquareAvatarWithRoundedCorners(userInfo.getUsername(), 30);
//        avatar= new ImageView(image);
//
//        this.getChildren().addAll(nameText,avatar);

    }


    public ConversationInfoCard(UserInfo userInfo) {

        var image = AvatarGenerator.generateSquareAvatarWithRoundedCorners(userInfo.getUsername(), 30);
        nameText = new Text(userInfo.getUsername());

        avatar = new ImageView(image != null ? image : new Image("default-avatar.png"));
        avatar.setFitWidth(50);
        avatar.setFitHeight(50);
        avatar.setPreserveRatio(true);

        this.getChildren().addAll(nameText,avatar);


    }



    public ConversationInfoCard(String name, String email, Image avatarImage) {
        // 创建头像
        avatar = new ImageView(avatarImage != null ? avatarImage : new Image("default-avatar.png"));
        avatar.setFitWidth(50);
        avatar.setFitHeight(50);
        avatar.setPreserveRatio(true);

        // 创建姓名文本

    }
}
