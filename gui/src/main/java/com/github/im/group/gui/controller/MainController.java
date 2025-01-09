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

import static io.github.palexdev.materialfx.MFXResourcesLoader.loadURL;

@Service
@Slf4j
@FxView(path = "main_layout")
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
        chatMainPane.initialize(null, null);
        rootpane.setCenter(chatMainPane);


        closeIcon.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> Platform.exit());
        minimizeIcon.addEventHandler(MouseEvent.MOUSE_CLICKED, event ->
                ((Stage) rootpane.getScene().getWindow()).setIconified(true));

//        alwaysOnTopIcon.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> {
//            boolean newVal = !stage.isAlwaysOnTop();
//            alwaysOnTopIcon.pseudoClassStateChanged(PseudoClass.getPseudoClass("always-on-top"), newVal);
//            stage.setAlwaysOnTop(newVal);
//        });

        sendMessageButton.setButtonType(ButtonType.FLAT);

//        sendMessageButton.setOnAction(event -> sendMessage());

        // Example action for loading chat content
//        conversationList.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
//            if (newValue != null) {
//                loadChatContent(newValue);
//            }
//        });
    }

//    private static ToggleButton createToggle(String icon, String text) {
//        return createToggle(icon, text, 0);
//    }

//    private static ToggleButton createToggle(String icon, String text, double rotate) {
//        MFXIconWrapper wrapper = new MFXIconWrapper(icon, 24, 32);
//        MFXRectangleToggleNode toggleNode = new MFXRectangleToggleNode(text, wrapper);
//        toggleNode.setAlignment(Pos.CENTER_LEFT);
//        toggleNode.setMaxWidth(Double.MAX_VALUE);
////        toggleNode.setToggleGroup(toggleGroup);
//        toggleNode.setToggleGroup(new ToggleGroup());
//        if (rotate != 0) wrapper.getIcon().setRotate(rotate);
//        return toggleNode;
//    }

    // Load the friend list from the backend
//    private void loadFriendList() {
//        log.debug("Loading friend list...");
//
//        Long userId = UserInfoContext.getCurrentUser().getUserId();
//        Mono<List<FriendshipDTO>> friendListMono = friendShipEndpoint.getFriends(userId);
//
//        friendListMono.doOnError(error -> log.error("Failed to load friends", error))
//                .doOnSuccess(this::updateFriendList)
//                .subscribe();
//    }
//
//    // Update the UI with the loaded friend list
//    private void updateFriendList(List<FriendshipDTO> friendships) {
//        Platform.runLater(() -> {
//            conversationList.getItems().clear();
//            friendships.forEach(friendship -> {
//                String friendName = friendship.getFriendUserInfo().getUsername();
//                conversationList.getItems().add(friendName);
//            });
//        });
//    }

//    // Load chat content for a selected conversation
//    private void loadChatContent(String friendName) {
//        log.debug("Loading chat content for: " + friendName);
//
//        chatContent.clear();
//        // Example: Simulate fetching chat history (replace with backend call)
//        chatContent.appendText("Chat with " + friendName + "\n");
//        chatContent.appendText("Friend: Hi there!\n");
//        chatContent.appendText("You: Hello!\n");
//    }

    // Send a message

}
