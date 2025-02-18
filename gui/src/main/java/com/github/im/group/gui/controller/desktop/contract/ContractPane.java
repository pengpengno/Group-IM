package com.github.im.group.gui.controller.desktop.contract;

import com.github.im.dto.user.FriendshipDTO;
import com.github.im.dto.user.UserInfo;
import com.github.im.group.gui.api.FriendShipEndpoint;
import com.github.im.group.gui.api.UserEndpoint;
import com.github.im.group.gui.context.UserInfoContext;
import com.gluonhq.charm.glisten.control.CharmListView;
import javafx.application.Platform;
import javafx.scene.image.Image;
import javafx.scene.layout.HBox;
import javafx.scene.control.Label;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class ContractPane extends HBox {

    private final UserEndpoint userEndpoint;
    private final FriendShipEndpoint friendShipEndpoint;

    private CharmListView<FriendshipDTO, String> contractListView;

    public void initialize() {
        // 初始化CharmListView
        contractListView = new CharmListView<>();

        // 设置联系人分组（例如按状态或姓名）
        contractListView.setHeadersFunction(friendship -> friendship.getFriendUserInfo().getUsername().substring(0, 1).toUpperCase());

        // 设置每个列表项的显示方式
        contractListView.setCellFactory(param -> new AccountCardCell());

        // 设置占位符
        contractListView.setPlaceholder(new Label("No contacts available"));

        // 加载好友数据并更新列表
        loadContacts();
    }

    // 加载联系人数据
    private void loadContacts() {
//        List<FriendshipDTO> friendships = fetchFriendships();
        friendShipEndpoint.getFriends(UserInfoContext.getCurrentUser().getUserId())
                .collectList()
                .subscribe(friendships -> updateFriendList(friendships));
    }

    // 更新好友列表
    private void updateFriendList(List<FriendshipDTO> friendships) {
        Platform.runLater(() -> {

            contractListView = new CharmListView<>();
            contractListView.setCellFactory(param -> new AccountCardCell());

        });
    }

    // 模拟加载好友数据
//    private List<FriendshipDTO> fetchFriendships() {
//        return List.of(
//                new FriendshipDTO(new UserInfo("John Doe", "john@example.com", "Online", new Image("avatar1.png"))),
//                new FriendshipDTO(new UserInfo("Jane Smith", "jane@example.com", "Offline", new Image("avatar2.png")))
//        );
//    }
}
