package com.github.im.group.gui.controller.desktop.contract;

import com.github.im.dto.user.FriendRequestDto;
import com.github.im.dto.user.FriendshipDTO;
import com.github.im.dto.user.UserInfo;
import com.github.im.group.gui.api.FriendShipEndpoint;
import com.github.im.group.gui.api.UserEndpoint;
import com.github.im.group.gui.context.UserInfoContext;
import com.gluonhq.charm.glisten.control.CharmListView;
import jakarta.annotation.PostConstruct;
import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
//public class ContractMainPane extends BorderPane  {
public class ContractMainPane extends GridPane {

    private final UserEndpoint userEndpoint;

    private final DetailInfoPane detailInfoPane;

    private final FriendShipEndpoint friendShipEndpoint;
    // 好友信息展示
    private CharmListView<FriendshipDTO, String> contractListView;

    private ListView<AccountCard> accountCardListView; // 好友列表

    private BorderPane friendsPane;

    // 搜索文本框
    private TextField title;


    public void initialize() {

        // 初始化CharmListView
//        contractListView = new CharmListView<>();
//
//        // 设置联系人分组（例如按状态或姓名）
//        contractListView.setHeadersFunction(friendship -> friendship.getFriendUserInfo().getUsername().substring(0, 1).toUpperCase());
//
//        // 设置每个列表项的显示方式
//        contractListView.setCellFactory(param -> new AccountCardCell());
//
//        // 设置占位符
//        contractListView.setPlaceholder(new Label("No contacts available"));

        // 加载好友数据并更新列表
        loadContacts();
    }



    @PostConstruct
    public void initComponent() {
        // 初始化


        friendsPane = new BorderPane();
        title = new TextField();

        accountCardListView = new ListView<>();

        friendsPane.setTop(title);
        friendsPane.setCenter(accountCardListView);

        title.setPromptText("搜索用户");

        // 设置 GridPane 布局   两列一行 2 * 1
        //  第一行 窄一些 第二列 宽一些  friendsPane 设置再最左边

        this.add(friendsPane, 0, 0); // 例如将好友列表放到 GridPane 的第 0 行 0 列
        this.add(detailInfoPane,1,0);


        // 设置列宽，确保 UI 不会挤在一起
        this.getColumnConstraints().add(new ColumnConstraints(100));  // 设置第 0 列宽度为 250

        ColumnConstraints col2 = new ColumnConstraints();
        col2.setHgrow(javafx.scene.layout.Priority.ALWAYS);  // 让第 1 列可以自动扩展

        detailInfoPane.setMaxWidth(Double.MAX_VALUE);


        //  响应式的取 friendsPane 占用完毕后的剩余宽度
//        detailInfoPane.prefWidthProperty().
        detailInfoPane.prefWidthProperty().bind(this.widthProperty().subtract(friendsPane.widthProperty()));


        this.setVgap(10);  // 设置行间距
        this.setHgap(10);  // 设置列间距

        // 设置朋友面板宽度为固定值，例如 300
        friendsPane.setPrefWidth(300);  // 设置 friendsPane 宽度
        friendsPane.setMaxWidth(400);  // 设置 friendsPane 最大宽度


//        initialize();
    }


    private void queryUser(){
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

        friendShipEndpoint.getFriends(UserInfoContext.getCurrentUser().getUserId())
                .collectList()
                .subscribe(this::updateFriendList);

    }

    // 更新好友列表
    private void updateFriendList(List<FriendshipDTO> friendships) {
        Platform.runLater(() -> {
            log.info("更新好友列表");
            var collect = friendships.stream()
                    .map(fri-> {
                        var accountCard = new AccountCard((fri));
                        accountCard.setOnMouseClicked(event-> {
                            detailInfoPane.display(accountCard.getUserInfo());
                        });
                        return accountCard;
                    })
//                    .forEach(e-> e.setOnMouseClicked(event-> detailInfoPane.display(e.getUserInfo())))
                    .toList();

            // 先 清楚现在有的数据
            accountCardListView.getItems().clear();

            accountCardListView.getItems().addAll(collect);

            contractListView = new CharmListView<>();
            contractListView.setCellFactory(param -> new AccountCardCell());

        });
    }


}
