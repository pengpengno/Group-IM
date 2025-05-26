package com.github.im.group.gui.controller.desktop.contract;

import com.github.im.dto.user.FriendshipDTO;
import com.github.im.dto.user.UserInfo;
import com.github.im.group.gui.util.AvatarGenerator;
import com.gluonhq.charm.glisten.control.Avatar;
import javafx.animation.PauseTransition;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Text;
import javafx.stage.Popup;
import javafx.stage.PopupWindow;
import javafx.util.Duration;
import lombok.Getter;

/**
 * AccountCard 用于展示用户简略信息，如头像和姓名
 */
//public class AccountCard extends PopupWindow {
public class AccountCard extends StackPane {

//    private final ImageView avatar;
    private final Avatar avatar;
    private final Label nameLabel;
    
    private final HoverCard hoverCard;

    @Getter
    private final UserInfo userInfo;

    public AccountCard(UserInfo userInfo) {
//        super();
        this.userInfo = userInfo;

        // 生成头像
        avatar = AvatarGenerator.getAvatar(userInfo.getUsername(), AvatarGenerator.AvatarSize.SMALL);
//        this.avatar = new ImageView(image);
//        this.avatar.setFitWidth(30);
//        this.avatar.setFitHeight(30);

        // 用户名
        this.nameLabel = new Label(userInfo.getUsername());

        // 布局：将头像和名字放到 HBox 里
        HBox contentBox = new HBox(10, avatar, nameLabel);

        // 可选样式
        contentBox.setStyle("-fx-padding: 8px; -fx-alignment: center-left;");

        // HoverCard 初始化（默认不显示）
        this.hoverCard = new HoverCard(userInfo.getUsername(), userInfo.getUsername(), avatar);
//        hoverCard.setVisible(false);  // 初始隐藏
        hoverCard.setMouseTransparent(true); // 不拦截鼠标事件
//        var popupWindow = new PopupWindow();
        var popup = new Popup();
        popup.getContent().add(hoverCard);
        popup.setAutoHide(true); // 鼠标点击其他区域时自动隐藏
        popup.setHideOnEscape(true);
        popup.setAnchorLocation(PopupWindow.AnchorLocation.WINDOW_TOP_LEFT);
        // 控制 hoverCard 显示/隐藏
        this.setOnMouseEntered(e ->{
            popup.show(this, e.getScreenX() +10, e.getScreenY() +10 );//避免挡住指针
        });
        this.setOnMouseExited(e -> {
            // 延迟一点隐藏，避免切到 popup 时误触
            PauseTransition delay = new PauseTransition(Duration.millis(200));
            delay.setOnFinished(ev -> {
                popup.hide();
            });
            delay.play();
        });

        // 将两层叠加：底层是主内容，顶层是 hoverCard
        this.getChildren().addAll(contentBox);
    }

}
