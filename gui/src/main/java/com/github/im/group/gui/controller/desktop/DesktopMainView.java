package com.github.im.group.gui.controller.desktop;

import com.github.im.common.connect.enums.PlatformType;
import com.github.im.group.gui.context.UserInfoContext;
import com.github.im.group.gui.controller.Display;
import com.github.im.group.gui.controller.MainHomeView;
import com.github.im.group.gui.controller.PlatformView;
import com.github.im.group.gui.controller.desktop.chat.ChatMainPane;
import com.github.im.group.gui.controller.desktop.contract.ContractMainPane;
import com.github.im.group.gui.controller.desktop.menu.impl.AbstractMenuButton;
import com.github.im.group.gui.util.AvatarGenerator;
import com.github.im.group.gui.util.I18nUtil;
import com.gluonhq.charm.glisten.application.AppManager;
import com.gluonhq.charm.glisten.control.AppBar;
import com.gluonhq.charm.glisten.control.FloatingActionButton;
import com.gluonhq.charm.glisten.control.Icon;
import com.gluonhq.charm.glisten.control.ToggleButtonGroup;
import com.gluonhq.charm.glisten.mvc.View;
import com.gluonhq.charm.glisten.visual.MaterialDesignIcon;
import io.github.palexdev.materialfx.controls.MFXButton;
import io.github.palexdev.materialfx.enums.ButtonType;
import io.github.palexdev.materialfx.utils.ToggleButtonsUtil;
import io.github.palexdev.mfxresources.fonts.MFXFontIcon;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.ResourceBundle;

@Slf4j
@Service
public class DesktopMainView  extends View implements MainHomeView {

    @Autowired
    private ChatMainPane chatMainPane;


    @Autowired
    private ContractMainPane contractMainPane;


    @Autowired
    private AbstractMenuButton abstractMenuButton;

    @FXML
    private VBox iconMenu;

    @FXML
    private ImageView avatar;
    @FXML
    private BorderPane borderPane;
    @FXML
    private GridPane rootpane;
    @FXML
    private HBox windowHeader;

    @FXML
    private StackPane chatPane;
    @FXML
    private TextArea chatContent;
    @FXML
    private TextField messageInput;
    @FXML
    private MFXButton sendMessageButton;


    @FXML
//    private MFXButton closeIcon;
    private MFXFontIcon closeIcon;
    @FXML
//    private MFXButton minimizeIcon;
    private MFXFontIcon minimizeIcon;
    @FXML
//    private MFXButton alwaysOnTopIcon;
    private MFXFontIcon alwaysOnTopIcon;



    private final ToggleGroup toggleGroup = new ToggleGroup();



    private ResourceBundle bundle = ResourceBundle.getBundle("i18n.menu.button");


    /**
     * 切换主 Panel
     * @param displayPanel
     */
    public void switchRootPane (Node displayPanel) {


        borderPane.setCenter(displayPanel);

    }

    @Override
    protected void updateAppBar(AppBar appBar) {
        super.updateAppBar(appBar);

        var icon = new Icon();
        icon.setContent(MaterialDesignIcon.CLOSE);

        appBar.setNavIcon(MaterialDesignIcon.MENU.button(e -> System.out.println("nav icon")));

        appBar.setTitleText("The AppBar");

        appBar.getActionItems().addAll(
                MaterialDesignIcon.SEARCH.button(e -> System.out.println("search")),
                MaterialDesignIcon.FAVORITE.button(e -> System.out.println("fav")));

        appBar.getMenuItems().addAll(new MenuItem("Settings"));

    }



    public ImageView windowIcon(String iconName) {
        var node = new ImageView(new Image(I18nUtil.getInputSteamByBundleName(bundle, iconName)));
        node.setFitWidth(12);
        node.setFitHeight(12);
        return node;
    }

    private void initWindowIcons() {

//        chatMainPane.loadFriendList();
//        borderPane.setLeft(chatMainPane);

//        borderPane.setCenter(new TextArea("sadjsajkdjsajkd"));

        FloatingActionButton fab = new FloatingActionButton();
        fab.showOn(this);

        fab.setOnAction(event -> {
            System.out.println("click");
        });
//        this.getChildren().add(fab);

        var appBar = getAppManager().getAppBar();

        // 新增条件判断，仅在非客户端桌面端更新 AppBar
//        if (!getPlatform().equals(PlatformType.CLIENT_DESKTOP)) {
            appBar.setTitleText("The AppBar");
            appBar.getActionItems().addAll(
                    MaterialDesignIcon.SEARCH.button(e -> System.out.println("search")),
                    MaterialDesignIcon.FAVORITE.button(e -> System.out.println("fav")));

            appBar.getMenuItems().addAll(new MenuItem("Settings"));

            appBar.getMenuItems().addAll(new MenuItem("Settings"));
//        }
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
                ((Stage) rootpane.getScene().getWindow()).setIconified(true));

        // 置顶/取消置顶窗口
        alwaysOnTopIcon.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> {
            Stage stage = (Stage) rootpane.getScene().getWindow();
            boolean alwaysOnTop = stage.isAlwaysOnTop();
            stage.setAlwaysOnTop(!alwaysOnTop);
//            alwaysOnTopIcon.setStyle(alwaysOnTop ? "-fx-fill: gray;" : "-fx-fill: blue;");
        });


    }

    @FXML
    public void initialize() {

        // 加载好友列表并设置到主界面

//        rootpane.setCenter(chatMainPane);

        initWindowIcons();

        var appBar = AppManager.getInstance().getAppBar();
        updateAppBar(appBar);
//        appBar.setVisible(false);

        // 设置发送按钮样式
        sendMessageButton.setButtonType(ButtonType.FLAT);

        UserInfoContext.subscribeUserInfoSink()
            .subscribe(userInfo -> {
                log.info("更新头像");
                var image = AvatarGenerator.generateSquareAvatarWithRoundedCorners(userInfo.getUsername(), 50);
//                    var image = AvatarGenerator.generateSquareAvatarWithRoundedCorners(userInfo.getUsername(), 50, Color.valueOf("#2196F3"));
                Platform.runLater(() -> avatar.setImage(image));
            });


        // 初始化菜单按钮
        setupMenuButtons();
    }

    /**
     * 初始化左侧菜单栏按钮，并添加 Tooltip
     */
    private void setupMenuButtons() {
        var allButtons = abstractMenuButton.getAllButtons();
        allButtons.forEach(button -> {
            button.setToggleGroup(toggleGroup);
        });

//        toggleGroup.getToggles().addAll(AbstractMenuButton.getAllButtons());

//        ToggleButtonsUtil.addAlwaysOneSelectedSupport(toggleGroup);

        iconMenu.getChildren().addAll(allButtons);

    }



    @Override
    public PlatformType getPlatform() {
        return PlatformType.DESKTOP;
    }

}
