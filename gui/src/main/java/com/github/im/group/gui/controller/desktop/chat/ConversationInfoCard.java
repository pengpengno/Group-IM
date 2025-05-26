package com.github.im.group.gui.controller.desktop.chat;

import com.github.im.conversation.ConversationRes;
import com.github.im.dto.user.FriendshipDTO;
import com.github.im.dto.user.UserInfo;
import com.github.im.group.gui.controller.desktop.contract.HoverCard;
import com.github.im.group.gui.util.AvatarGenerator;
import com.gluonhq.charm.glisten.control.Avatar;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import lombok.Getter;
import org.springframework.util.CollectionUtils;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 *  用于展示会话简略信息，如头像和姓名
 */
//public class ConversationInfoCard extends StackPane {
public class ConversationInfoCard extends HBox {

    private Label nameLabel;
    private Label lastMessageLabel;
//    private ImageView avatar;
    private Avatar avatar;

    @Getter
    private ConversationRes conversation;

    public ConversationInfoCard(ConversationRes item, String groupName) {
        this.conversation = item;

        // 头像
        avatar = AvatarGenerator.getAvatar(groupName, AvatarGenerator.AvatarSize.MEDIUM);
//        avatar = new ImageView(image);
//        avatar.setFitWidth(40);  // 控制头像大小
//        avatar.setFitHeight(40);
//        avatar.setPreserveRatio(true);

        // 左边头像部分
        StackPane avatarPane = new StackPane(avatar);
        avatarPane.setPrefSize(50, 50);  // 预留头像区空间
        avatarPane.setMinSize(50, 50);
        avatarPane.setMaxSize(50, 50);

        // 右边信息部分
        nameLabel = new Label(groupName);
        nameLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");

        lastMessageLabel = new Label("最新消息...");
        lastMessageLabel.setStyle("-fx-text-fill: gray; -fx-font-size: 12px;");

        VBox messageBox = new VBox(nameLabel, lastMessageLabel);
        messageBox.setSpacing(5);
        messageBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        this.getChildren().addAll(avatarPane, messageBox);
        this.setSpacing(10);
        this.setPadding(new Insets(10));
        this.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        this.getStyleClass().add("conversation-card");
    }

    public void setClickAction(Mono<Void> clickAction) {
        this.setOnMouseClicked(event -> {
            if (clickAction != null) {
                clickAction.subscribe();
            }
        });
    }
}
