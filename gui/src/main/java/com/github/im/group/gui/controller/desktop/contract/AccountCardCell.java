package com.github.im.group.gui.controller.desktop.contract;

import com.github.im.dto.user.FriendshipDTO;
import com.github.im.dto.user.UserInfo;
import com.github.im.group.gui.util.AvatarGenerator;
import com.gluonhq.charm.glisten.control.CharmListCell;
import com.gluonhq.charm.glisten.control.ListTile;
import com.gluonhq.charm.glisten.visual.MaterialDesignIcon;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;

public class AccountCardCell extends CharmListCell<FriendshipDTO> {

    private final ListTile tile;
    private FriendshipDTO currentItem;
    private final HBox buttonBar;

    public AccountCardCell() {
        tile = new ListTile();
        tile.setPrimaryGraphic(MaterialDesignIcon.ACCOUNT_CIRCLE.graphic());

        // 创建头像和姓名显示区域
        Label nameLabel = new Label();
        tile.setTextLine(1,nameLabel.textProperty().getValue());

        // 创建操作按钮区域（例如编辑/删除等）
        buttonBar = new HBox(5);
        buttonBar.setAlignment(Pos.CENTER_RIGHT);

        // 设置辅助区域
        tile.setSecondaryGraphic(buttonBar);

    }

    @Override
    public void updateItem(FriendshipDTO item, boolean empty) {
        super.updateItem(item, empty);
        currentItem = item;

        if (empty || item == null) {
            setGraphic(null);
        } else {
            UserInfo userInfo = item.getFriendUserInfo();

            // 更新ListTile的主要内容（头像和用户名）
            tile.textProperty().set(0,userInfo.getUsername());
            var image = AvatarGenerator.generateCircleAvatar(userInfo.getUsername(), 50);
            var imageView = new ImageView(image);
            tile.setPrimaryGraphic(imageView);

            // 添加更多信息或交互按钮（如编辑、删除等）
            buttonBar.getChildren().clear();
            // 这里可以添加按钮进行操作，例如编辑、删除等
//             buttonBar.getChildren().add(MaterialDesignIcon.EDIT.button(e -> editItem(item)));

            // 设置当前图形内容
            setGraphic(tile);
        }
    }
}
