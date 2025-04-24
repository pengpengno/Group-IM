package com.github.im.group.gui.controller.desktop.chat;

import com.github.im.group.gui.context.UserInfoContext;
import com.github.im.group.gui.util.AvatarGenerator;
import com.jfoenix.controls.JFXTextArea;
import io.github.palexdev.materialfx.controls.MFXTextField;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import org.fxmisc.richtext.GenericStyledArea;
import org.fxmisc.richtext.InlineCssTextArea;

/**
 * Description:
 * <p>
 *     message bubble pane
 *     a chat message bubble pane should contains
 *     1. sender
 *     2. message text
 * </p>
 *
 * @author pengpeng
 * @version 1.0
 * @since 2025/1/14
 */
public class ChatBubblePane extends HBox {


    private String sender;

    private String message;

    // 创建头像
    private ImageView avatar ;

    private Label senderLabel;


//    private TextArea senderTextField;
    private InlineCssTextArea senderTextField;
//    private Text senderTextField;


    /**
     *
     * @param message message text
     * @param name  sender
     * @param isSent  isCurrentSender , if true , the avatar is on the right side
     */
    private void init(String message, String name , boolean isSent) {

        this.setSpacing(10);
        this.setPadding(new Insets(10));
        this.setAlignment(isSent ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);

        avatar = new ImageView(AvatarGenerator.generateCircleAvatar(name, 100));
        avatar.setFitWidth(40);
        avatar.setFitHeight(40);

        senderTextField = new InlineCssTextArea();
        senderTextField.setEditable(false);
        senderTextField.appendText(message);
        senderTextField.setWrapText(true);
        senderTextField.setMaxWidth(250);

        // 动态调整高度
        senderTextField.textProperty().addListener((obs, oldText, newText) -> adjustTextAreaHeight(senderTextField));
        adjustTextAreaHeight(senderTextField); // 初始调整

        HBox messageBubble = new HBox(senderTextField);
        messageBubble.setPadding(new Insets(10));
        messageBubble.getStyleClass().add("message-bubble");
        if (isSent) {
            messageBubble.getStyleClass().add("sent-message-bubble");
        }

        messageBubble.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(senderTextField, Priority.ALWAYS);  // 允许扩展

        VBox messageBox = new VBox(5);
        VBox.setVgrow(senderTextField, Priority.ALWAYS);

        messageBox.getChildren().add(messageBubble);

//        messageBox.getChildren().add(senderTextField);
//
        if (isSent) {
            this.getChildren().addAll(messageBox, avatar);
        } else {
            this.getChildren().addAll(avatar, messageBox);
        }
    }


    private ChatBubblePane(){

    }


    protected ChatBubblePane(String message){

        var currentUser = UserInfoContext.getCurrentUser();

        this.sender = currentUser.getUsername();

        init(message, sender, true);

    }

    public ChatBubblePane(String sender, String message) {
        init(message, sender, false);
    }



    private static void adjustTextAreaHeight(TextArea textArea) {

        // 计算文本的高度
        double textHeight = textArea.getFont().getSize() * textArea.getText().split("\n").length + 10;

        // 设置高度（确保在最小和最大高度范围内）
        double newHeight = Math.min(Math.max(textHeight, textArea.getMinHeight()), textArea.getMaxHeight());
        textArea.setPrefHeight(newHeight);
    }

    private void adjustTextAreaHeight(InlineCssTextArea textArea) {
        Platform.runLater(() -> {
            double textHeight = computeTextHeight(textArea);
            textArea.setPrefHeight(textHeight); // 设置合适的高度
        });
    }

    /**
     * 计算 InlineCssTextArea 需要的高度
     */
    private double computeTextHeight(InlineCssTextArea textArea) {
        int lines = textArea.getText().split("\n").length;  // 计算文本行数
        double lineHeight = 18; // 估算单行高度（可根据字体调整）
        return Math.max(40, lines * lineHeight + 10); // 最小高度 40
    }





}