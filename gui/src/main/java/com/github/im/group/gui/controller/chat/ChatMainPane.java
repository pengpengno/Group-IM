package com.github.im.group.gui.controller.chat;

import com.github.im.dto.user.FriendshipDTO;
import com.github.im.group.gui.api.FriendShipEndpoint;
import com.github.im.group.gui.context.UserInfoContext;
import io.github.palexdev.materialfx.controls.MFXTextField;
import jakarta.annotation.PostConstruct;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

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

    private VBox chatVBox ; // 防止 拉取的 好友会话列表的 VBox

    private ListView<ChatInfoPane> conversationList;
//    private ListView<String> conversationList;

    private final ChatMessagePane chatPane;

    private final FriendShipEndpoint friendShipEndpoint;





    public static class ChatInfoPane extends AnchorPane implements Initializable{

        private Label chatNameLabel ;


        private Label  recentMessageLabel ; // 最近的 消息


        public ChatInfoPane(String chatName){
            chatNameLabel = new Label(chatName);
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
                String friendName = friendship.getFriendUserInfo().getUsername();
                conversationList.getItems().add(new ChatInfoPane(friendName));
            });
        });
    }



    public  void switchChatPane(){

        // 切换不同的聊天窗体
    }

    @PostConstruct
    public void initComponent() {

        // 初始化
        conversationList = new ListView<>();

        this.setLeft(conversationList);

        chatPane.initialize(null,null);
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {


        loadFriendList();


    }

    // Load the friend list from the backend
    private void loadFriendList() {
        log.debug("Loading friend list...");

        Long userId = UserInfoContext.getCurrentUser().getUserId();
        Mono<List<FriendshipDTO>> friendListMono = friendShipEndpoint.getFriends(userId);


        // Example action for loading chat content
        conversationList.getSelectionModel().selectedItemProperty()
                .addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                var chatNameLabel = newValue.chatNameLabel;
                log.info("click {}",chatNameLabel);
//                loadChatContent(newValue);
            }
        });
        friendListMono.doOnError(error -> log.error("Failed to load friends", error))
                .doOnSuccess(this::updateFriendList)
                .subscribe();
    }




}