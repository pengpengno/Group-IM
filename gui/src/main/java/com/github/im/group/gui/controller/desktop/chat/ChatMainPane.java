package com.github.im.group.gui.controller.desktop.chat;

import com.github.im.dto.user.FriendshipDTO;
import com.github.im.dto.user.UserInfo;
import com.github.im.group.gui.api.FriendShipEndpoint;
import com.github.im.group.gui.context.UserInfoContext;
import jakarta.annotation.PostConstruct;
import javafx.application.Platform;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Lookup;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Description:
 * <p>
 *     chat mould main pane
 *     include
 *     <ul>
 *         <li> friends list </li>
 *         <li> Chat Message Pane </li>
 *     </ul>
 * </p>
 *
 * @author pengpeng
 * @version 1.0
 * @since 2025/1/9
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChatMainPane extends BorderPane implements Initializable {


    private ListView<ChatInfoPane> conversationList;

    private ChatMessagePane currentChatPane;

    private final FriendShipEndpoint friendShipEndpoint;

    private ConcurrentHashMap<String, ChatMessagePane>  chatPaneMap = new ConcurrentHashMap<>();



    public static class ChatInfoPane extends AnchorPane implements Initializable{

        private Label chatNameLabel ;
        @Getter
//        @Setter
        private UserInfo userInfo;

        private Label  recentMessageLabel ; // 最近的 消息


        public ChatInfoPane(UserInfo userInfo){
            var username = userInfo.getUsername();
            chatNameLabel = new Label(username);
            this.getChildren().add(chatNameLabel);
        }

        @Override
        public void initialize(URL location, ResourceBundle resources) {


        }
    }

    /**
     * 更新 好友列表
     * @param friendships
     */
    private void updateFriendList(List<FriendshipDTO> friendships) {
        Platform.runLater(() -> {
            conversationList.getItems().clear();
            friendships.forEach(friendship -> {
                var friendUserInfo = friendship.getFriendUserInfo();
                var chatInfoPane = new ChatInfoPane(friendUserInfo);
                chatInfoPane.setOnMouseClicked(mouseEvent -> {
                    log.debug("click chatInfo pane");
                    var chatMessagePane = getChatMessagePane(friendUserInfo);
                    switchChatPane(chatMessagePane);
                });
                conversationList.getItems().add(chatInfoPane);
            });
        });
    }

    public ChatMessagePane getChatMessagePane (UserInfo userInfo) {

        var username = userInfo.getUsername();

        if (chatPaneMap.containsKey(username)){
            return chatPaneMap.get(username);
        }else{
            var newChatPane = createChatMessagePane();
//            newChatPane.initialize();
            newChatPane.setToAccountInfo(userInfo);
            chatPaneMap.putIfAbsent(username, newChatPane);
            return newChatPane;
        }


    }

    @Lookup
    protected ChatMessagePane createChatMessagePane() {
        // Spring 会自动注入此方法的实现，无需手动实现
        return null;
    }


    /**
     * 切换聊天的窗体
     * @param chatMessagePane
     */
    public void switchChatPane(ChatMessagePane chatMessagePane){

        Platform.runLater(() -> {
            currentChatPane = chatMessagePane;

            if (currentChatPane != null){
                this.setCenter(currentChatPane);
            }
        });


    }

    @PostConstruct
    public void initComponent() {

        // 初始化
        conversationList = new ListView<>();

        this.setLeft(conversationList);

    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {

//        loadFriendList();

    }

    // Load the friend list from the backend
    public void loadFriendList() {
        log.debug("Loading friend list...");

        Long userId = UserInfoContext.getCurrentUser().getUserId();
        Mono<List<FriendshipDTO>> friendListMono = friendShipEndpoint.getFriends(userId);


        friendListMono.doOnError(error -> log.error("Failed to load friends", error))
                .doOnSuccess(this::updateFriendList)
                .subscribe();
    }





}