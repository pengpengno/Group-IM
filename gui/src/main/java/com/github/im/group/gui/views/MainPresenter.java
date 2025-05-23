package com.github.im.group.gui.views;

import com.github.im.common.connect.connection.client.ClientToolkit;
import com.github.im.common.connect.enums.PlatformType;
import com.github.im.common.connect.model.proto.Account;
import com.github.im.common.connect.model.proto.BaseMessage;
import com.github.im.group.gui.context.UserInfoContext;
import com.github.im.group.gui.controller.MainHomeView;
import com.github.im.group.gui.controller.PlatformView;
import com.github.im.group.gui.controller.desktop.chat.ChatMainPresenter;
import com.github.im.group.gui.controller.desktop.menu.impl.AbstractMenuButton;
import com.github.im.group.gui.util.AvatarGenerator;
import com.github.im.group.gui.util.FxView;
import com.github.im.group.gui.util.I18nUtil;
import com.github.im.group.gui.util.ViewUtils;
import com.gluonhq.charm.glisten.application.AppManager;
import com.gluonhq.charm.glisten.control.*;
import com.gluonhq.charm.glisten.mvc.View;
import com.gluonhq.charm.glisten.visual.MaterialDesignIcon;
import io.github.palexdev.mfxresources.fonts.MFXFontIcon;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.MenuItem;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.ResourceBundle;

@Slf4j
@Service
@RequiredArgsConstructor
@FxView(fxmlName = "main_layout")
public class MainPresenter  implements MainHomeView {
//public class MainPresenter  implements MainHomeView, Initializable {

    private final AbstractMenuButton abstractMenuButton;

    @FXML
    private VBox iconMenu;

    @FXML
    private Avatar avatar;

    @FXML
    private BorderPane borderPane;

    @FXML
    private View rootPane;

    @FXML
    private HBox windowHeader;

    @FXML
    private MFXFontIcon closeIcon;
    @FXML
//    private MFXButton minimizeIcon;
    private MFXFontIcon minimizeIcon;
    @FXML
//    private MFXButton alwaysOnTopIcon;
    private MFXFontIcon alwaysOnTopIcon;


    @Getter
    private final ToggleGroup toggleGroup = new ToggleGroup();


    private ResourceBundle bundle = ResourceBundle.getBundle("i18n.menu.button");

    private final ChatMainPresenter chatMainPresenter;

    /**
     * 切换主 Panel
     * @param displayPanel
     */
    public void switchRootPane (Node displayPanel) {

        borderPane.setCenter(displayPanel);

    }


    public BottomNavigation bottomNavigation () {
        BottomNavigation bottomNav = new BottomNavigation();

        var type = bottomNav.getType();
        // 创建底部按钮
        BottomNavigationButton message =
                new BottomNavigationButton("聊天", MaterialDesignIcon.MESSAGE.graphic());


        BottomNavigationButton people =
                new BottomNavigationButton("联系人", MaterialDesignIcon.PEOPLE.graphic());


        BottomNavigationButton mine =
                new BottomNavigationButton("我的", MaterialDesignIcon.PERSON.graphic());
//        loginView.setCenter(peopleView);
        people.setSelected(true);

        // 添加按钮到底部导航栏
        bottomNav.getActionItems().addAll(message,people,mine);

        return bottomNav;

    }

    public void initialAppBar() {

        // 底部导航栏
        var isMobile = !com.gluonhq.attach.util.Platform.isDesktop();
        // 移动端底部栏  客户端侧边栏
        if(isMobile){

            rootPane.setBottom(bottomNavigation());

        }else{

            rootPane.setBottom(bottomNavigation());

        }

        rootPane.showingProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue) {

                AppBar appBar = AppManager.getInstance().getAppBar();

                appBar.setNavIcon(MaterialDesignIcon.MENU.button(e -> System.out.println("nav icon")));

                appBar.setTitleText("The AppBar");

                appBar.getActionItems().addAll(
                        MaterialDesignIcon.SEARCH.button(e -> System.out.println("search")),
                        MaterialDesignIcon.FAVORITE.button(e -> System.out.println("fav")));

                appBar.getMenuItems().addAll(new MenuItem("Settings"));

                appBar.setNavIcon(MaterialDesignIcon.MENU.button(e ->
                        AppManager.getInstance().getDrawer().open()));

                appBar.setTitleText("主页");
            }
        });
    }


    public ImageView windowIcon(String iconName) {
        var node = new ImageView(new Image(I18nUtil.getInputSteamByBundleName(bundle, iconName)));
        node.setFitWidth(12);
        node.setFitHeight(12);
        return node;
    }

    private void initWindowIcons33() {


        FloatingActionButton fab = new FloatingActionButton();
//        fab.showOn(this);

        fab.setOnAction(event -> {
            System.out.println("click");
        });
//        this.getChildren().add(fab);

//        var node = new ImageView(new Image(I18nUtil.getInputSteamByBundleName(bundle, "close.icon")));
//        node.setFitWidth(12);
//        node.setFitHeight(12);
//        closeIcon.setGraphic(windowIcon("close.icon"));
//        var node1 = new ImageView(new Image(I18nUtil.getInputSteamByBundleName(bundle, "mini.icon")));
//        minimizeIcon.setGraphic(windowIcon("mini.icon"));
//        alwaysOnTopIcon.setGraphic(new ImageView(new Image(I18nUtil.getInputSteamByBundleName(bundle, "onTop.icon"))));
//        alwaysOnTopIcon.setGraphic(windowIcon("onTop.icon"));
//        closeIcon.setGraphic(new ImageView(new Image(I18nUtil.getInputSteamByBundleName(bundle, "close.icon"))));
//        minimizeIcon.setGraphic(new ImageView(new Image(Objects.requireNonNull(getClass().getResourceAsStream(bundle.getString("mini.icon"))))));
//        alwaysOnTopIcon.setGraphic(new ImageView(new Image(Objects.requireNonNull(getClass().getResourceAsStream(bundle.getString("onTop.icon"))))));

        // 关闭窗口
        closeIcon.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> Platform.exit());
//        closeIcon.setText(null);
//        minimizeIcon.setText(null);
//        alwaysOnTopIcon.setText(null);
        // 最小化窗口
        minimizeIcon.addEventHandler(MouseEvent.MOUSE_CLICKED, event ->
                ((Stage) rootPane.getScene().getWindow()).setIconified(true));

        // 置顶/取消置顶窗口
        alwaysOnTopIcon.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> {
            Stage stage = (Stage) rootPane.getScene().getWindow();
            boolean alwaysOnTop = stage.isAlwaysOnTop();
            stage.setAlwaysOnTop(!alwaysOnTop);
//            alwaysOnTopIcon.setStyle(alwaysOnTop ? "-fx-fill: gray;" : "-fx-fill: blue;");
        });


    }




//    @Override
//    public void initialize(URL location, ResourceBundle resources) {
    public void initialize() {
        initialAppBar();
        setupMenuButtons();
        var userInfo = UserInfoContext.getCurrentUser();
        if (userInfo != null){
            // 已经登录 且存在 那么就直接 绘画出头像
            var image = AvatarGenerator.generateSquareAvatarWithRoundedCorners(userInfo.getUsername(), 50);
//                    var image = AvatarGenerator.generateSquareAvatarWithRoundedCorners(userInfo.getUsername(), 50, Color.valueOf("#2196F3"));
            Platform.runLater(() -> avatar.setImage(image));
        }else{
            // 不存在就挂载个响应式 订阅事件
            UserInfoContext.subscribeUserInfoSink()
                    .subscribe(userInfoFromSink -> {
                        log.info("更新头像");
                        var image = AvatarGenerator.generateSquareAvatarWithRoundedCorners(userInfoFromSink.getUsername(), 50);
//                    var image = AvatarGenerator.generateSquareAvatarWithRoundedCorners(userInfo.getUsername(), 50, Color.valueOf("#2196F3"));
                        Platform.runLater(() -> avatar.setImage(image));
                    });
        }

    }


    /**
     * 初始化左侧菜单栏按钮，并添加 Tooltip
     */
    private void setupMenuButtons() {

        var buttonTuples = abstractMenuButton.getAllButtonTuplesList();

        var eventSink = abstractMenuButton.getEventSink();

        List<ToggleButton> allButtons = buttonTuples.stream().map(buttonTup -> {
            var button =  buttonTup.getT2();

            button.setToggleGroup(toggleGroup);

            // eventSink 接收到对应的事件，然后toggleGroup 激活对应的按钮 触发事件

            // 订阅事件流，触发按钮对应的状态
            eventSink.subscribe(event -> {
                if (buttonTup.getT1().equals(event)) {
                    log.debug(" button switch event {}",event.getSimpleName());

                    toggleGroup.selectToggle(button);

                    button.fireEvent(new ActionEvent(this,button));
                }
            });

            button.setPadding(Insets.EMPTY); // 去除按钮的 padding

            return button;

        }) .toList();

        var reConnected =  new Button("重新连接");
        reConnected.setOnMouseClicked(event-> {
            var userInfo = UserInfoContext.getCurrentUser();
            var accountInfo = Account.AccountInfo.newBuilder()
                    .setPlatformType(PlatformView.getCurrentPlatformType())
                    .setUserId(userInfo.getUserId())
                    .setAccountName(userInfo.getUsername())
                    .setAccount(userInfo.getUsername())
                    .setEMail(userInfo.getEmail())
                    .build();

            var baseMessage = BaseMessage.BaseMessagePkg.newBuilder()
                    .setAccountInfo(accountInfo)
                    .build();

            ClientToolkit.reactiveClientAction()
                    .sendMessage(baseMessage)
                    .subscribe();
        });
        iconMenu.getChildren().addAll(allButtons);
        iconMenu.getChildren().add(reConnected);

    }




    @Override
    public PlatformType getPlatform() {
        return PlatformType.DESKTOP;
    }


}
