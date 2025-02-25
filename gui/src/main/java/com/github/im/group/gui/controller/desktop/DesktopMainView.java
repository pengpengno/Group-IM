package com.github.im.group.gui.controller.desktop;

import com.github.im.common.connect.enums.PlatformType;
import com.github.im.group.gui.context.UserInfoContext;
import com.github.im.group.gui.controller.MainHomeView;
import com.github.im.group.gui.controller.PlatformView;
import com.github.im.group.gui.controller.desktop.chat.ChatMainPane;
import com.github.im.group.gui.util.AvatarGenerator;
import io.github.palexdev.materialfx.controls.MFXButton;
import io.github.palexdev.materialfx.enums.ButtonType;
import io.github.palexdev.mfxresources.fonts.MFXFontIcon;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.ResourceBundle;

@Slf4j
@Service
public class DesktopMainView implements MainHomeView {

    @Autowired
    private ChatMainPane chatMainPane;

    @FXML
    private VBox iconMenu;

    @FXML
    private ImageView avatar;
    @FXML
    private BorderPane rootpane;
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
    private MFXFontIcon closeIcon;
    @FXML
    private MFXFontIcon minimizeIcon;
    @FXML
    private MFXFontIcon alwaysOnTopIcon;


    private ResourceBundle bundle = ResourceBundle.getBundle("i18n/main");


    @FXML
    public void initialize() {

        // 加载好友列表并设置到主界面
        chatMainPane.loadFriendList();
        rootpane.setCenter(chatMainPane);

        // 关闭窗口
        closeIcon.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> Platform.exit());

        // 最小化窗口
        minimizeIcon.addEventHandler(MouseEvent.MOUSE_CLICKED, event ->
                ((Stage) rootpane.getScene().getWindow()).setIconified(true));

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
//        iconMenu.getChildren().clear(); // 先清空已有按钮

        // 定义菜单项（图标路径 & 提示文本）
        List<String[]> menuItems = List.of(
                new String[]{"images/main/toolbox/message.png", "Chat"},
                new String[]{"images/main/toolbox/mail.png", "Mail"},
                new String[]{"images/main/toolbox/contacts.png", "Contacts"},
                new String[]{"images/main/toolbox/file.png", "Documents"},
                new String[]{"images/main/toolbox/schedule.png", "Schedule"},
                new String[]{"images/main/toolbox/meeting.png", "Meetings"},
                new String[]{"images/main/toolbox/workbench.png", "Workbench"}
        );
        iconMenu.setPadding(new Insets(0,0,0,0)); // 移除内边距
        iconMenu.setSpacing(0); // 按钮无间距

        iconMenu.setAlignment(Pos.TOP_CENTER); // 让头像和按钮紧贴
        for (String[] item : menuItems) {
            var button = createMenuButton(item[0], item[1]);
            iconMenu.getChildren().add(button);

        }

    }



    @Override
    public PlatformType getPlatform() {
        return PlatformType.DESKTOP;
    }


    /**
     * 创建菜单按钮
     */
    private Button createMenuButton(String iconPath, String tooltipText) {

        ImageView icon = new ImageView(new Image(Objects.requireNonNull(getClass().getResourceAsStream("/" + iconPath))));
        icon.setFitWidth(24);
        icon.setFitHeight(24);

        MFXButton button = new MFXButton();
//        Button button = new Button();
        button.setText(null);
//        button.setButtonType(ButtonType.RAISED);
        button.setGraphic(icon);
        button.setPrefSize(50, 50);
        button.setTooltip(new Tooltip(tooltipText));
        button.setPadding(new Insets(0,0,0,0));
        return button;
    }
}
