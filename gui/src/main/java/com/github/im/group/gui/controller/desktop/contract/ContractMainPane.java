package com.github.im.group.gui.controller.desktop.contract;

import com.github.im.dto.user.FriendRequestDto;
import com.github.im.dto.user.FriendshipDTO;
import com.github.im.dto.user.UserInfo;
import com.github.im.group.gui.api.FriendShipEndpoint;
import com.github.im.group.gui.api.UserEndpoint;
import com.github.im.group.gui.context.UserInfoContext;
import com.gluonhq.charm.glisten.control.CharmListView;
import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.ResourceBundle;

@Component
@RequiredArgsConstructor
@Slf4j
public class ContractMainPane extends BorderPane  {

    private final UserEndpoint userEndpoint;

    private final FriendShipEndpoint friendShipEndpoint;
    // 好友信息展示
    private CharmListView<FriendshipDTO, String> contractListView;

    // 搜索文本框
    private TextField title;


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



    private  void queryUser(){
        // 根据 搜索框传入 搜索的文本  用户姓名或者 邮箱来检索
        userEndpoint.queryUser(title.getText())
                .collectList()
                .subscribe(friendships -> {
                    //  在 contractListView 展示所有的 返回的检索用户信息
                });
    }

    /**
     * 添加 用户好友
     */
    private void addUser(UserInfo  friendUserInfo){
        var currentUser = UserInfoContext.getCurrentUser();
        var friendshipDTO = new FriendRequestDto();
        friendshipDTO.setUserId(currentUser.getUserId());
        friendshipDTO.setAccount(currentUser.getUsername());
        friendshipDTO.setUserName(currentUser.getUsername());

        friendshipDTO.setFriendAccount(friendUserInfo.getUsername());
        friendshipDTO.setFriendName(friendUserInfo.getUsername());
        friendshipDTO.setFriendId(friendUserInfo.getUserId());

        friendShipEndpoint
                .sendFriendRequest(friendshipDTO)
                .subscribe(aVoid -> {
                    log.info("发送好友请求成功");
                });

    }
    // 加载联系人数据
    private void loadContacts() {
//        List<FriendshipDTO> friendships = fetchFriendships();
        friendShipEndpoint.getFriends(UserInfoContext.getCurrentUser().getUserId())
                .collectList()
                .subscribe(this::updateFriendList);
    }

    // 更新好友列表
    private void updateFriendList(List<FriendshipDTO> friendships) {
        Platform.runLater(() -> {

            contractListView = new CharmListView<>();
            contractListView.setCellFactory(param -> new AccountCardCell());

        });
    }


}
