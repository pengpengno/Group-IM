package com.github.im.group.gui.controller.desktop.contract;

import com.github.im.dto.user.FriendshipDTO;
import com.github.im.dto.user.UserInfo;
import com.github.im.group.gui.util.AvatarGenerator;
import com.gluonhq.charm.glisten.control.CharmListCell;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.scene.control.Label;

public class AccountCardCell extends CharmListCell<FriendshipDTO> {

    private final VBox vbox = new VBox();
    private final Label nameLabel = new Label();
//    private final Label statusLabel = new Label();
    private final ImageView avatar = new ImageView();

    public AccountCardCell() {
        vbox.getChildren().addAll(avatar, nameLabel);
    }

    @Override
    public void updateItem(FriendshipDTO item, boolean empty) {
        super.updateItem(item, empty);
        if (empty || item == null) {
            setGraphic(null);
        } else {
            UserInfo userInfo = item.getFriendUserInfo();

            // 更新显示信息
            nameLabel.setText(userInfo.getUsername());
//            statusLabel.setText(userInfo.getStatus());  // 或根据需要调整显示其他字段
            avatar.setImage(AvatarGenerator.generateCircleAvatar(userInfo.getUsername(), 50));

            setGraphic(vbox);  // 设置渲染项为 VBox
        }
    }
}
