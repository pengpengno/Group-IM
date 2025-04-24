package com.github.im.group.gui.controller.desktop.chat;

import com.github.im.conversation.ConversationRes;
import com.github.im.dto.session.MessageDTO;
import com.github.im.dto.session.MessagePullRequest;
import com.github.im.dto.user.FriendshipDTO;
import com.github.im.dto.user.UserInfo;
import com.github.im.enums.ConversationType;
import com.github.im.group.gui.api.ConversationEndpoint;
import com.github.im.group.gui.api.FriendShipEndpoint;
import com.github.im.group.gui.api.MessageEndpoint;
import com.github.im.group.gui.context.UserInfoContext;
import jakarta.annotation.PostConstruct;
import javafx.application.Platform;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Lookup;
import org.springframework.context.ApplicationContext;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.web.PagedModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

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
public class ChatMainPane extends GridPane implements Initializable {


//    private ListView<ChatInfoPane> conversationList;
    private ListView<ConversationInfoCard> conversationList;


    private Set<Long> conversationIdSet = ConcurrentHashMap.newKeySet();

    private ChatMessagePane currentChatPane;

    private ConcurrentHashMap<String, ChatMessagePane>  chatPaneMap = new ConcurrentHashMap<>();

    private final FriendShipEndpoint friendShipEndpoint;
    private final ConversationEndpoint conversationEndpoint;
    private final MessageEndpoint messagesEndpoint;




    private final ApplicationContext applicationContext;

    /**
     * 更新会话列表
     * 此方法用于处理接收到的新会话，如果会话已存在，则切换到该会话，如果不存在，则创建新的会话卡片并添加到会话列表中
     *
     * @param conversation 会话信息对象，包含会话的详细信息
     */
    public void updateConversations(ConversationRes conversation ) {

        // 在JavaFX线程中执行UI更新操作
        Platform.runLater(() -> {

            // 获取会话ID
            var conversationId = conversation.getConversationId();
            // 检查会话ID是否已存在
            if (conversationIdSet.contains(conversationId)){
                // 如果会话已存在，获取并切换到对应的会话界面
                var chatMessagePane = getChatMessagePane(conversationId);
                switchChatPane(chatMessagePane);
                return;
            }

            // 初始化群组显示名称
            String groupDisplayName = "";
            // 获取会话类型
            var conversationType = ConversationType.valueOf(conversation.getConversationType());
            // 根据会话类型处理不同的情况
            switch(conversationType){

                case PRIVATE_CHAT -> {
                    // 私聊情况下，获取会话成员，并找出不是当前用户的朋友信息
                    groupDisplayName = getDisplayGroupName(conversation);
                }
                case GROUP->{
                    groupDisplayName = conversation.getGroupName();
                }

            }

            // 记录日志，添加新的会话用户名
            log.debug("add new Conversation username = {}",groupDisplayName);
            // 创建新的会话信息卡片
            var conversationInfoCard = new ConversationInfoCard(conversation,groupDisplayName);
            // 设置会话卡片的点击事件处理
            conversationInfoCard.setOnMouseClicked(mouseEvent -> {
                log.debug("click chatInfo pane");
                var chatMessagePane = getChatMessagePane(conversationId);
                switchChatPane(chatMessagePane);
            });
            // 点击拉取 历史会话
            conversationInfoCard.setClickAction(loadHistoryMessages(conversationId));


            // 将新的会话卡片添加到会话列表中
            conversationList.getItems().add(conversationInfoCard);
            // 将新的会话ID添加到会话ID集合中
            conversationIdSet.add(conversationId);

        });

    }


    /**
     * 获取对话的显示名称
     * 根据对话类型获取显示名称私聊时显示对方用户名，其他情况显示群组名称
     *
     * @param conversation 对话对象，包含对话类型和成员信息等
     * @return 对话的显示名称
     */
    public String getDisplayGroupName(ConversationRes conversation) {
        // 获取对话类型
        var conversationType = conversation.getConversationType();

        // 判断对话类型是否为私聊
        if(conversationType.equals(ConversationType.PRIVATE_CHAT.name())){
            // 获取对话成员列表
            var members = conversation.getMembers();

            // 在私聊中，过滤掉当前用户，获取对方用户的名字如果找不到则返回空字符串
            return members.stream()
                    .filter(item -> !item.getUserId().equals(UserInfoContext.getCurrentUser().getUserId()))
                    .findFirst()
                    .map(UserInfo::getUsername)
                    .orElse("");
        }else{
            // 非私聊情况下，直接返回对话的群组名称
            return conversation.getGroupName();
        }
    }

    /**
     * 加载会话列表
     */
    public Mono<Void> loadConversation(UserInfo userInfo) {
        return  Mono.justOrEmpty(userInfo)
                .flatMap(e-> {
                    return conversationEndpoint.getActiveConversationsByUserId(e.getUserId())
                    .doOnNext(actions -> {
                        actions.stream().forEach(action -> updateConversations(action));
                    })
                    .doOnError(throwable -> {
                        log.error("load conversation error",throwable);
                    })
                            .then()
                    ;
                });
    }

    public Mono<Void> loadHistoryMessages(Long conversationId) {
        if(conversationId == null){
            return Mono.empty();
        }
        var messagePullRequest = new MessagePullRequest();
        messagePullRequest.setConversationId(conversationId);
        var chatMessagePane = getChatMessagePane(conversationId);

//        var entityModelPagedModel = ;
        return Mono.fromCallable(() -> messagesEndpoint.pullHistoryMessages(messagePullRequest))
                .map(model-> model.getContent())
                .doOnNext(models -> chatMessagePane.addMessages(models))
                .then();

//        var collect = entityModelPagedModel.getContent()
//                .stream().map(e -> e.getContent())
//                .collect(Collectors.toList());
//        chatMessagePane.addMessages(collect);
//        return  entityModelPagedModel
//                .doOnNext(e->{
//
//                    chatMessagePane.addMessages(e.stream().toList());
//                })
//                .doOnError(throwable -> {
//                    log.error("load history messages error",throwable);
//                })
//                .then()
//;
    }



    /**
     * 根据会话ID获取聊天消息面板
     * 如果该会话ID对应的聊天面板已存在，则直接返回现有的聊天面板
     * 如果不存在，则创建一个新的聊天面板，设置其会话ID，并将其添加到聊天面板映射中
     *
     * @param conversationId 会话ID，用于唯一标识一个聊天会话
     * @return 返回与该会话ID对应的聊天消息面板实例
     */
    public ChatMessagePane getChatMessagePane (Long conversationId) {
        // 将会话ID转换为字符串形式，以便在映射中使用
        var key = String.valueOf(conversationId);
        // 检查映射中是否已存在该会话ID对应的聊天面板
        var contains =   chatPaneMap.containsKey(key);
        if (contains){
            // 如果存在，则直接返回该聊天面板
            return chatPaneMap.get(key);
        }else{
            // 如果不存在，则创建一个新的聊天面板实例
            var newChatPane = createChatMessagePane();
            // 设置新聊天面板的会话ID
            newChatPane.setConversationId(conversationId);
            // 将新创建的聊天面板添加到映射中，如果映射中已存在该键，则不执行任何操作
            chatPaneMap.putIfAbsent(key, newChatPane);
            // 返回新创建的聊天面板
            return newChatPane;
        }
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
    public void switchChatPane(ChatMessagePane chatMessagePane) {
        Platform.runLater(() -> {
            if (currentChatPane != null) {
                this.getChildren().remove(currentChatPane); // 先移除旧的聊天面板
            }
            currentChatPane = chatMessagePane;

            // 重新添加新的聊天面板
            this.add(currentChatPane, 1, 0);

            // 绑定宽度，确保 UI 自适应
            currentChatPane.prefWidthProperty().bind(this.widthProperty().subtract(conversationList.widthProperty()));
        });
    }


    @PostConstruct
    public void initComponent() {
        // 初始化
        conversationList = new ListView<>();

//        currentChatPane = new ChatMessagePane();
        currentChatPane = applicationContext.getBean(ChatMessagePane.class);

        this.add(currentChatPane,1,0);

        this.add(conversationList, 0, 0); // 例如将好友列表放到 GridPane 的第 0 行 0 列


        // 设置列宽，确保 UI 不会挤在一起
        this.getColumnConstraints().add(new ColumnConstraints(100));  // 设置第 0 列宽度为 250

        ColumnConstraints col2 = new ColumnConstraints();
        col2.setHgrow(javafx.scene.layout.Priority.ALWAYS);  // 让第 1 列可以自动扩展



        UserInfoContext.subscribeUserInfoSink().flatMap(this::loadConversation).subscribe();


    }



    @Override
    public void initialize(URL location, ResourceBundle resources) {

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
//                var chatInfoPane = new ConversationInfoCard(friendUserInfo);
                var chatInfoPane = new ConversationInfoCard(friendUserInfo);
                chatInfoPane.setOnMouseClicked(mouseEvent -> {
                    log.debug("click chatInfo pane");
                    var chatMessagePane = getChatMessagePane(friendUserInfo);
                    switchChatPane(chatMessagePane);
                });
                conversationList.getItems().add(chatInfoPane);
            });
        });
    }






}