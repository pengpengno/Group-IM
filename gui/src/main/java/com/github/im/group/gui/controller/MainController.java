package com.github.im.group.gui.controller;

import com.github.im.common.connect.connection.client.ClientToolkit;
import com.github.im.common.connect.model.proto.Account;
import com.github.im.common.connect.model.proto.BaseMessage;
import com.github.im.common.connect.model.proto.Chat;
import com.github.im.dto.user.FriendshipDTO;
import com.github.im.dto.user.UserInfo;
import com.github.im.group.gui.api.FriendShipEndpoint;
import com.github.im.group.gui.context.UserInfoContext;
import com.github.im.group.gui.controller.chat.ChatMainPane;
import com.github.im.group.gui.util.FxView;
import com.github.im.group.gui.util.FxmlLoader;
import io.github.palexdev.materialfx.controls.MFXButton;
import io.github.palexdev.materialfx.controls.MFXIconWrapper;
import io.github.palexdev.materialfx.controls.MFXRectangleToggleNode;
import io.github.palexdev.materialfx.enums.ButtonType;
import io.github.palexdev.materialfx.utils.ScrollUtils;
import io.github.palexdev.materialfx.utils.others.loader.MFXLoader;
import io.github.palexdev.materialfx.utils.others.loader.MFXLoaderBean;
import io.github.palexdev.mfxresources.fonts.MFXFontIcon;
import javafx.application.Platform;
import javafx.css.PseudoClass;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import static io.github.palexdev.materialfx.MFXResourcesLoader.loadURL;

@Service
@Slf4j
@FxView(path = "main_layout",viewName = "MAIN_VIEW")
public class MainController {

    @Autowired
    private FriendShipEndpoint friendShipEndpoint;

    @Autowired
    private ChatMainPane chatMainPane;

    @FXML
    private VBox iconMenu;
    @FXML
    private BorderPane rootpane ;


    @FXML
    private HBox windowHeader ;
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
//        chatMainPane.initialize(null, null);
        chatMainPane.loadFriendList();
        rootpane.setCenter(chatMainPane);

        closeIcon.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> Platform.exit());
        minimizeIcon.addEventHandler(MouseEvent.MOUSE_CLICKED, event ->
                ((Stage) rootpane.getScene().getWindow()).setIconified(true));

        sendMessageButton.setButtonType(ButtonType.FLAT);

    }


}
