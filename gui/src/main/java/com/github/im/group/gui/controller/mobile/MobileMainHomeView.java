package com.github.im.group.gui.controller.mobile;

import com.github.im.group.gui.controller.desktop.chat.ChatMainPane;
import com.github.im.group.gui.controller.desktop.menu.impl.AbstractMenuButton;
import io.github.palexdev.materialfx.controls.MFXButton;
import io.github.palexdev.materialfx.enums.ButtonType;
import io.github.palexdev.mfxresources.fonts.MFXFontIcon;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
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

@Service
@Slf4j
public class MobileMainHomeView {

    @Autowired
    private ChatMainPane chatMainPane;

    @FXML
    private VBox iconMenu;
    @FXML
    private BorderPane rootpane;
    @FXML
    private HBox windowHeader;
    @FXML
    private ListView<String> conversationList;
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




    @FXML
    public void initialize() {
        // 加载好友列表并设置到主界面
//        chatMainPane.loadFriendList();
//        rootpane.setCenter(chatMainPane);

        // 关闭窗口
        closeIcon.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> Platform.exit());

        // 最小化窗口
        minimizeIcon.addEventHandler(MouseEvent.MOUSE_CLICKED, event ->
                ((Stage) rootpane.getScene().getWindow()).setIconified(true));

        // 设置发送按钮样式
        sendMessageButton.setButtonType(ButtonType.FLAT);

        // 初始化菜单按钮
        setupMenuButtons();
    }

    /**
     * 初始化左侧菜单栏按钮，并添加 Tooltip
     */
    private void setupMenuButtons() {
        iconMenu.getChildren().clear(); // 先清空已有按钮

        // 定义菜单项（图标路径 & 提示文本）
//        List<String[]> menuItems = List.of(
//                new String[]{"images/main/toolbox/mail.png", "Mail"},
//                new String[]{"images/main/toolbox/file.png", "Documents"},
//                new String[]{"images/main/toolbox/schedule.png", "Schedule"},
//                new String[]{"images/main/toolbox/meeting.png", "Meetings"},
//                new String[]{"images/main/toolbox/workbench.png", "Workbench"},
//                new String[]{"images/main/toolbox/contacts.png", "Contacts"}
//        );
//

//        iconMenu.getChildren().addAll(AbstractMenuButton.getAllButtons());
//        for (String[] item : menuItems) {
//            MFXButton button = createMenuButton(item[0], item[1]);
//            iconMenu.getChildren().add(button);
//        }
    }


    /**
     * 创建菜单按钮
     */
    private MFXButton createMenuButton(String iconPath, String tooltipText) {
        ImageView icon = new ImageView(new Image(getClass().getResourceAsStream("/" + iconPath)));
        icon.setFitWidth(24);
        icon.setFitHeight(24);

        MFXButton button = new MFXButton();
        button.setGraphic(icon);
        button.setPrefSize(50, 50);
        button.getStyleClass().add("menu-button"); // 确保有 CSS 样式
        button.setTooltip(new Tooltip(tooltipText));

        return button;
    }
}
