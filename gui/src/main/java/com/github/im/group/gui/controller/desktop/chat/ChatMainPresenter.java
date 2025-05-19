package com.github.im.group.gui.controller.desktop.chat;

import com.github.im.conversation.ConversationRes;
import com.github.im.dto.PageResult;
import com.github.im.dto.session.MessagePullRequest;
import com.github.im.dto.user.UserInfo;
import com.github.im.enums.ConversationType;
import com.github.im.group.gui.api.ConversationEndpoint;
import com.github.im.group.gui.api.FileEndpoint;
import com.github.im.group.gui.api.MessageEndpoint;
import com.github.im.group.gui.connect.handler.EventBus;
import com.github.im.group.gui.context.UserInfoContext;
import com.github.im.group.gui.controller.desktop.chat.messagearea.MessageNodeService;
import com.github.im.group.gui.util.ViewUtils;
import com.github.im.group.gui.views.AppViewManager;
import com.github.im.group.gui.views.MenuItem;
import com.github.im.group.gui.views.ViewLifeCycle;
import com.gluonhq.charm.glisten.application.AppManager;
import com.gluonhq.charm.glisten.control.AppBar;
import com.gluonhq.charm.glisten.mvc.View;
import com.gluonhq.charm.glisten.visual.MaterialDesignIcon;
import jakarta.annotation.PostConstruct;
import javafx.application.Platform;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.ListView;
import javafx.scene.control.SplitPane;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Lookup;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.net.URL;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Description:  Chat 页面的主体
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
//@Scope("sin" )
@RequiredArgsConstructor
//public class ChatMainPane extends GridPane implements Initializable, ApplicationContextAware {
//public class ChatMainPresenter extends View implements Initializable,ApplicationContextAware, MenuItem {
public class ChatMainPresenter extends View implements ApplicationContextAware, MenuItem {

    @Getter
    private SplitPane mainPane ;

    private ListView<ConversationInfoCard> conversationList;

    private ChatMessagePane currentChatPane;

    private final Set<Long> conversationIdSet = ConcurrentHashMap.newKeySet();

    private final ConcurrentHashMap<String, ChatMessagePane>  chatPaneMap = new ConcurrentHashMap<>();

    private final ConversationEndpoint conversationEndpoint;
    private final MessageEndpoint messagesEndpoint;
    private final EventBus eventBus;

    private final FileEndpoint fileEndpoint;
    private final MessageNodeService messageNodeService;


    private ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(@NonNull ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;

        var view = AppViewManager.createView(this);
        view.registerView();
        ViewUtils.buildDrawer(view);
//        AppViewManager.registerViewsAndDrawer();
    }


    @Override
    public String title() {
        return "聊天";
    }

    @Override
    public ViewLifeCycle.Flag[] flags() {
        return new ViewLifeCycle.Flag[]{
                ViewLifeCycle.Flag.SHOW_IN_DRAWER,
                ViewLifeCycle.Flag.SKIP_VIEW_STACK
        };
    }

    @Override
    public Node menuIcon() {
        return MaterialDesignIcon.MESSAGE.graphic();
    }

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
                loadHistoryMessages(conversationId).subscribe();
            });
            // 点击拉取 历史会话
//            conversationInfoCard.setClickAction(loadHistoryMessages(conversationId));

            // 将新的会话卡片添加到会话列表中
            conversationList.getItems().add(conversationInfoCard);
            // 将新的会话ID添加到会话ID集合中
            conversationIdSet.add(conversationId);

        });

    }



    @Override
    protected void updateAppBar(AppBar appBar) {
        super.updateAppBar(appBar);

        appBar.setNavIcon(MaterialDesignIcon.MENU.button(e -> System.out.println("nav icon")));

        appBar.setTitleText("The AppBar");

        appBar.getActionItems().addAll(
                MaterialDesignIcon.SEARCH.button(e -> System.out.println("search")),
                MaterialDesignIcon.FAVORITE.button(e -> System.out.println("fav")));

        appBar.getMenuItems().addAll(new javafx.scene.control.MenuItem("Settings"));

        appBar.setNavIcon(MaterialDesignIcon.MENU.button(e ->
                AppManager.getInstance().getDrawer().open()));

        appBar.setTitleText("聊天");
//        initComponent();
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
                        actions.forEach(action -> updateConversations(action));
                    })
                    .doOnError(throwable -> {
                        log.error("load conversation error",throwable);
                    })
                    .then()
                    ;
                });
    }

    /**
     * 加载历史消息
     * 根据会话ID加载对应的历史消息如果会话ID为空，则返回空的Mono对象
     *
     * @param conversationId 会话ID，用于标识特定的会话
     * @return 返回Mono<Void>，表示异步操作没有返回值
     */
    public Mono<Void> loadHistoryMessages(Long conversationId) {
        // 检查会话ID是否为空，如果为空则返回空的Mono对象
        if(conversationId == null){
            return Mono.empty();
        }

        // 创建消息拉取请求对象，并设置会话ID
        var messagePullRequest = new MessagePullRequest();
        messagePullRequest.setConversationId(conversationId);

        // 获取聊天消息面板
        var chatMessagePane = getChatMessagePane(conversationId);

        // TODO 拉取最新消息 msgId 与 本地 对应的MsgId 就视作 已经最新

        // 从消息端点拉取历史消息，并处理消息
        return Mono.fromCallable(() -> messagesEndpoint.pullHistoryMessages(messagePullRequest))
                .map(PageResult::getContent)
                .doOnNext(chatMessagePane::addMessages)
                .then()
//                .cache()//缓存这个操作就行了  后续的调用不执行
                ;
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
            var newChatPane = createChatMessagePane(conversationId);
            // 设置新聊天面板的会话ID
//            newChatPane.setConversationId(conversationId);
            // 将新创建的聊天面板添加到映射中，如果映射中已存在该键，则不执行任何操作
            chatPaneMap.putIfAbsent(key, newChatPane);
            // 返回新创建的聊天面板
            return newChatPane;
        }
    }


//    @Lookup
    protected ChatMessagePane createChatMessagePane(Long conversationId) {
        // Spring 会自动注入此方法的实现，无需手动实现
        var chatMessagePane = new ChatMessagePane(conversationId, eventBus, fileEndpoint, messageNodeService);
        chatMessagePane.initialize();
        return chatMessagePane;
//        return applicationContext.getBean(ChatMessagePane.class);
    }


    /**
     * 切换聊天的窗体
     * @param chatMessagePane
     */
    public void switchChatPane(ChatMessagePane chatMessagePane) {
        Platform.runLater(() -> {
            if (currentChatPane != null) {

                mainPane.getItems().remove(currentChatPane); // 先移除旧的聊天面板
            }
            currentChatPane = chatMessagePane;
            currentChatPane.setMinWidth(500);

            // 重新添加新的聊天面板

            mainPane.getItems().add(currentChatPane);
        });
    }


    @PostConstruct
    public void initComponent() {



        this.showingProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                log.info("ChatMainPresenter showing");
            } else {
                log.info("ChatMainPresenter hidden");
            }
        });

        mainPane = new SplitPane();
        eventBus.asFlux()
                .ofType(ConversationRes.class)
                .subscribe(conversationRes -> {
                    updateConversations(conversationRes);
                });
        // 初始化
        conversationList = new ListView<>();
        // 先使用个默认的空对象
        currentChatPane = new ChatMessagePane(null,null,null,null);
        currentChatPane.setMinWidth(500);

        conversationList.setMinWidth(170);
        conversationList.setPrefWidth(170);
        conversationList.setMaxWidth(300);
        mainPane.setDividerPositions(0.3); // 初始比例，30%：70%

        mainPane.getItems().addAll(conversationList,currentChatPane);
//

        UserInfoContext.subscribeUserInfoSink()
                .flatMap(this::loadConversation).subscribe();

        this.setCenter(mainPane);


    }



}