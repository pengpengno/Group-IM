package com.github.im.group.gui.controller.chat;

import com.github.im.common.connect.connection.client.ClientToolkit;
import com.github.im.common.connect.model.proto.Account;
import com.github.im.common.connect.model.proto.BaseMessage;
import com.github.im.common.connect.model.proto.Chat;
import com.github.im.group.gui.context.UserInfoContext;
import io.github.palexdev.materialfx.controls.MFXButton;
import io.github.palexdev.materialfx.controls.MFXScrollPane;
import io.github.palexdev.materialfx.enums.ButtonType;
import jakarta.annotation.PostConstruct;
import javafx.application.Platform;
import javafx.fxml.Initializable;
import javafx.scene.control.TextArea;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * Description:
 * <p>
 *     chat message pane , include  message send area , messageDisplay Area;
 *
 * </p>
 *
 * @author pengpeng
 * @version 1.0
 * @since 2025/1/9
 */
@Component
@Slf4j
public class ChatMessagePane extends BorderPane implements Initializable {


    private TextArea messageSendArea; // message send area


    private TextArea messageDisplayArea;


    private SendMessagePane sendMessagePane;

    public static class SendMessagePane extends AnchorPane implements Initializable {


        private MFXButton sendButton;

        private SendMessagePane(){
            initialize();
        }

        @Override
        public void initialize(URL location, ResourceBundle resources) {

        }
//        @PostConstruct
        public void initialize() {
            // 初始化  将文本域 放在 boderpane 最上方
            sendButton = new MFXButton("发送");
            sendButton.setButtonType(ButtonType.RAISED);

            sendButton.setLayoutX(250); // Example: Set position in AnchorPane
            sendButton.setLayoutY(10); // Example: Set position in AnchorPane

            this.getChildren().add(sendButton);
            this.setPrefHeight(50); // Set the height for the send area


        }




    }

    private void sendMessage() {
        String message = messageSendArea.getText();
        if (message == null || message.isBlank()) {
            return;
        }

        log.debug("Sending message: " + message);

        var userInfo = UserInfoContext.getCurrentUser();
        var accountInfo = Account.AccountInfo.newBuilder()
                .setUserId(userInfo.getUserId())
                .setAccountName(userInfo.getUsername())
                .build();

        Chat.ChatMessage chatMessage = Chat.ChatMessage.newBuilder()
                .setToAccountInfo(accountInfo)
                .setContent(message)
                .build();

        var baseChatMessage = BaseMessage.BaseMessagePkg.newBuilder()
                .setMessage(chatMessage)
                .build();

        ClientToolkit.reactiveClientAction().sendMessage(baseChatMessage).doOnSuccess(response -> {
            Platform.runLater(() -> {
//                    chatContent.appendText("You: " + message + "\n");
//                    messageInput.clear();
            });
        }).subscribe();
    }


    @Override
    public void initialize(URL location, ResourceBundle resources) {

    }

    @PostConstruct
    public void initialize() {
        // Initialize message display area
        messageDisplayArea = new TextArea();
        messageDisplayArea.setEditable(false); // Message display should be non-editable
        messageDisplayArea.setWrapText(true);

        // Initialize send message area
        messageSendArea = new TextArea();
        messageSendArea.setPromptText("Type a message..."); // Placeholder text

        // Create a scroll pane for message display area
        MFXScrollPane scrollPane = new MFXScrollPane(messageDisplayArea);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);

        // Place the message display area inside the center of BorderPane
        this.setCenter(scrollPane);

        // Place the send message area at the bottom of the BorderPane
        this.setBottom(messageSendArea);

        // Create a SendMessagePane instance and place it in the bottom-right corner
        SendMessagePane sendMessagePane = new SendMessagePane();
        this.setRight(sendMessagePane);

        sendMessagePane.sendButton.setOnAction(event -> {
            sendMessage();
        });

        // Optional: You can add logic to handle message sending with the send button
    }


    private void loadChatContent(String friendName) {
        log.debug("Loading chat content for: " + friendName);

//        chatContent.clear();
//        // Example: Simulate fetching chat history (replace with backend call)
//        chatContent.appendText("Chat with " + friendName + "\n");
//        chatContent.appendText("Friend: Hi there!\n");
//        chatContent.appendText("You: Hello!\n");
    }
}