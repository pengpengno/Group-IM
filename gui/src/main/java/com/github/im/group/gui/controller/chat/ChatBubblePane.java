package com.github.im.group.gui.controller.chat;

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
 *
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


//    private MFXTextField senderTextField;
//    private JFXTextArea senderTextField;
    private TextArea senderTextField;
//    private InlineCssTextArea senderTextField;




    public ChatBubblePane(String sender ,String message) {
        init(message, sender, false);
    }
    private void init(String message, String name , boolean isSent) {

        this.setSpacing(10); // 间距
        this.setPadding(new Insets(10));
        this.setAlignment(isSent ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT); // 根据消息类型调整对齐

        avatar = new ImageView( AvatarGenerator.generateAvatar(name, 100));;
        avatar.setFitWidth(40);
        avatar.setFitHeight(40);
//        avatar.setStyle("-fx-effect: dropshadow(gaussian, gray, 4, 0.5, 0, 0);"); // 添加阴影效果


        senderTextField = new JFXTextArea();
//        senderTextField = new InlineCssTextArea();
//        senderTextField = new TextArea();
//
        senderTextField.setEditable(false);
        senderTextField.appendText(message);
//        senderTextField.setText(message);
        senderTextField.setMaxWidth(250);

//        adjustTextAreaHeight(senderTextField);
        // 包裹消息文本的 TextFlow
        TextFlow messageBubble = new TextFlow(senderTextField);
        messageBubble.setPadding(new Insets(10));
        messageBubble.setStyle("-fx-background-color: " + (isSent ? "#4caf50" : "#2196f3") + ";"
                + "-fx-background-radius: 15;");
        messageBubble.setMaxWidth(250); // 限制最大宽度
        messageBubble.setLineSpacing(2);
//        InlineCssTextArea
//        this.getChildren().add(textFlow);


        // 消息部分 (包含发送者名称和气泡)
        VBox messageBox = new VBox(5);
//        if (!isSent) {
//            messageBox.getChildren().add(senderLabel);
//        }


        setHgrow(senderTextField, Priority.ALWAYS);

//        messageBox.getChildren().add(senderTextField);
        messageBox.getChildren().add(messageBubble);


        // 根据消息类型添加组件顺序
        if (isSent) {
            this.getChildren().addAll(messageBox, avatar); // 自己的消息：消息在左，头像在右
        } else {
            this.getChildren().addAll(avatar, messageBox); // 对方的消息：头像在左，消息在右
        }


    }

    private ChatBubblePane(){

    }

    protected ChatBubblePane(String message){

        var currentUser = UserInfoContext.getCurrentUser();

        this.sender = currentUser.getUsername();

        init(message, sender, true);

    }

    public ChatBubblePane(String sender, String message,  boolean isSent) {
        this.setSpacing(10); // 间距
        this.setPadding(new Insets(10));
        this.setAlignment(isSent ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT); // 根据消息类型调整对齐
        avatar = new ImageView( AvatarGenerator.generateAvatar(sender, 100));;

        avatar.setFitWidth(40);
        avatar.setFitHeight(40);
        avatar.setStyle("-fx-effect: dropshadow(gaussian, gray, 4, 0.5, 0, 0);"); // 添加阴影效果

        // 创建发送者名称

        senderLabel = new Label(sender);
        senderLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: gray;");

        // 创建消息文本
        Text messageText = new Text(message);
        messageText.setFill(Color.WHITE);

        // 包裹消息文本的 TextFlow
        TextFlow messageBubble = new TextFlow(messageText);
        messageBubble.setPadding(new Insets(10));
        messageBubble.setStyle("-fx-background-color: " + (isSent ? "#4caf50" : "#2196f3") + ";"
                + "-fx-background-radius: 15;");
        messageBubble.setMaxWidth(250); // 限制最大宽度
        messageBubble.setLineSpacing(2);
//        messageBubble

        // 消息部分 (包含发送者名称和气泡)
        VBox messageBox = new VBox(5);
        if (!isSent) {
            messageBox.getChildren().add(senderLabel);
        }
        messageBox.getChildren().add(messageBubble);

        // 根据消息类型添加组件顺序
        if (isSent) {
            this.getChildren().addAll(messageBox, avatar); // 自己的消息：消息在左，头像在右
        } else {
            this.getChildren().addAll(avatar, messageBox); // 对方的消息：头像在左，消息在右
        }

    }



    private static void adjustTextAreaHeight(TextArea textArea) {


        // 计算文本的高度
        double textHeight = textArea.getFont().getSize() * textArea.getText().split("\n").length + 10;

        // 设置高度（确保在最小和最大高度范围内）
        double newHeight = Math.min(Math.max(textHeight, textArea.getMinHeight()), textArea.getMaxHeight());
        textArea.setPrefHeight(newHeight);
    }




}