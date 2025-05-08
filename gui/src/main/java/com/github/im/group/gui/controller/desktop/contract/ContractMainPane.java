package com.github.im.group.gui.controller.desktop.contract;

import com.github.im.dto.user.FriendRequestDto;
import com.github.im.dto.user.FriendshipDTO;
import com.github.im.dto.user.UserInfo;
import com.github.im.group.gui.api.FriendShipEndpoint;
import com.github.im.group.gui.api.UserEndpoint;
import com.github.im.group.gui.context.UserInfoContext;
import com.gluonhq.charm.glisten.control.CharmListView;
import io.github.palexdev.materialfx.controls.MFXButton;
import jakarta.annotation.PostConstruct;
import javafx.application.Platform;
import javafx.scene.control.ListView;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClientException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.List;
import java.util.function.Function;

@Component
@RequiredArgsConstructor
@Slf4j
public class ContractMainPane extends SplitPane {

    private final UserEndpoint userEndpoint;

    private final DetailInfoPane detailInfoPane;

    private final FriendShipEndpoint friendShipEndpoint;
    // 好友信息展示
    private CharmListView<FriendshipDTO, String> contractListView;

    private ListView<AccountCard> accountCardListView; // 好友列表

    private BorderPane friendsPane;

    // 搜索文本框
    private TextField title;

    private MFXButton addButton ;

    private UserDisplayAndSearchPane userDisplayAndSearchPane;


    public void initialize() {

        // 加载好友数据并更新列表
        loadContacts();
    }


    public void setUpAddButton(){

        addButton = new MFXButton("添加");
        addButton.setOnAction(event -> {

            // 移除最后一个 使用 展示
            getItems().remove(1);
            getItems().add(userDisplayAndSearchPane);
//            this.add(userDisplayAndSearchPane,1,0);
        });
    }

    @PostConstruct
    public void initComponent() {
        // 初始化
        userDisplayAndSearchPane = new UserDisplayAndSearchPane();
        Function<String,List<UserInfo>> searchFunction = searchText -> {
           return userEndpoint.queryUser(searchText).block().getContent();
        };

        userDisplayAndSearchPane.setSerchFunction(searchFunction);
        userDisplayAndSearchPane.setAddButtonClickFunction(userInfo -> {
            sendAddRequest(userInfo);
        });

        setUpAddButton();

        friendsPane = new BorderPane();
        title = new TextField();
        title.setPromptText("搜索用户");

        var hBox = new HBox();
        hBox.getChildren().addAll(title,addButton);
        HBox.setHgrow(title, javafx.scene.layout.Priority.ALWAYS);

        accountCardListView = new ListView<>();
        friendsPane.setTop(hBox);
        friendsPane.setCenter(accountCardListView);



        // 设置 GridPane 布局   两列一行 2 * 1
        //  第一行 窄一些 第二列 宽一些  friendsPane 设置再最左边
//        this.add(friendsPane, 0, 0); // 例如将好友列表放到 GridPane 的第 0 行 0 列
//        this.add(detailInfoPane,1,0);


        // 设置列宽，确保 UI 不会挤在一起
//        var col1 = new ColumnConstraints(100);
//        this.getColumnConstraints().add(col1);  // 设置第 0 列宽度为 250

//        ColumnConstraints col2 = new ColumnConstraints();
//        col2.setHgrow(javafx.scene.layout.Priority.ALWAYS);  // 让第 1 列可以自动扩展

        detailInfoPane.setMaxWidth(Double.MAX_VALUE);


        //  响应式的取 friendsPane 占用完毕后的剩余宽度

//        detailInfoPane.prefWidthProperty()
//                .bind(this.widthProperty()
//                        .subtract(friendsPane.widthProperty()));


//        this.setVgap(10);  // 设置行间距
//        this.setHgap(10);  // 设置列间距

        // 设置朋友面板宽度为固定值，例如 300
        friendsPane.setPrefWidth(300);  // 设置 friendsPane 宽度
        friendsPane.setMaxWidth(400);  // 设置 friendsPane 最大宽度

        friendsPane.setMinWidth(200);
        detailInfoPane.setMinWidth(300);
        userDisplayAndSearchPane.setMinWidth(300);

        getItems().addAll(friendsPane, detailInfoPane);
        setDividerPositions(0.3); // 初始比例，30%：70%


    }




    /**
     * 发送添加好友的请求
     */
    private void sendAddRequest(UserInfo  friendUserInfo){
        var currentUser = UserInfoContext.getCurrentUser();
        var friendshipDTO = new FriendRequestDto();
        friendshipDTO.setUserId(currentUser.getUserId());

        friendshipDTO.setFriendId(friendUserInfo.getUserId());

        friendShipEndpoint
                .sendFriendRequest(friendshipDTO)
                .doOnNext(response -> {
                    int statusCode = response.getStatusCode().value();
                    log.info("发送好友请求状态码：{}", statusCode);
                    if (statusCode == 200) {
                        log.info("发送好友请求成功");
                    } else {
                        log.warn("好友请求响应异常，状态码：{}", statusCode);
                    }
                })
                .doOnError(WebClientResponseException.class, e-> {
                    var message = e.getMessage();
                    var statusCode = e.getStatusCode();
                    log.error("发送好友请求失败，状态码：{}，消息：{}", statusCode, message);
                })
//                .doOnError(e -> {
//                    log.error("发送好友请求失败", e);
//                })
                .onErrorComplete()
                .subscribe();

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
            var cards = friendships.stream()
                    .map(fri-> {
                        var accountCard = new AccountCard(fri.getFriendUserInfo());
                        accountCard.setOnMouseClicked(event-> {
                            detailInfoPane.display(accountCard.getUserInfo());
                        });
                        return accountCard;
                    })
//                    .forEach(e-> e.setOnMouseClicked(event-> detailInfoPane.display(e.getUserInfo())))
                    .toList();

            // 先 清除现在有的数据
            accountCardListView.getItems().clear();

            accountCardListView.getItems().addAll(cards);

            contractListView = new CharmListView<>();
            contractListView.setCellFactory(param -> new AccountCardCell());

        });
    }


}
