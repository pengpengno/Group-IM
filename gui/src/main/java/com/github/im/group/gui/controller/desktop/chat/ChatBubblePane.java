package com.github.im.group.gui.controller.desktop.chat;

import com.github.im.group.gui.context.UserInfoContext;
import com.github.im.group.gui.controller.desktop.MessageWrapper;
import com.github.im.group.gui.controller.desktop.chat.messagearea.MessageNodeService;
import com.github.im.group.gui.controller.desktop.chat.messagearea.richtext.MessageNode;
import com.github.im.group.gui.controller.desktop.chat.messagearea.richtext.RichTextMessageArea;
import com.github.im.group.gui.controller.desktop.chat.messagearea.richtext.file.FileNode;
import com.github.im.group.gui.controller.desktop.chat.messagearea.richtext.file.RemoteFileInfo;
import com.github.im.group.gui.util.AvatarGenerator;
import jakarta.annotation.Resource;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import lombok.RequiredArgsConstructor;
import org.fxmisc.richtext.GenericStyledArea;
import org.springframework.context.annotation.Scope;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.io.File;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

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
@Scope("prototype")
@Component
//@RequiredArgsConstructor
public class ChatBubblePane extends HBox {


    private String sender;

//    private String message;

    // 创建头像
    private ImageView avatar ;

    private Label senderLabel;

    private RichTextMessageArea senderTextField;

    private ProgressBar progressBar;

    private Label statusLabel;

    private MessageWrapper messageWrapper;  // 消息主体

    @Resource
    private MessageNodeService messageNodeService;


    private void uploadWithProgress(File file, UUID uploaderId) {
        WebClient client = WebClient.builder().baseUrl("http://localhost:8080").build();

        long totalBytes = file.length();
        Flux<DataBuffer> bufferFlux = DataBufferUtils.read(file.toPath(), new DefaultDataBufferFactory(), 4096);

        AtomicLong uploaded = new AtomicLong(0);

        bufferFlux = bufferFlux.map(dataBuffer -> {
            uploaded.addAndGet(dataBuffer.readableByteCount());
            double progress = uploaded.get() / (double) totalBytes;
            Platform.runLater(() -> {
                progressBar.setProgress(progress);
                statusLabel.setText(String.format("上传中：%.2f%%", progress * 100));
            });
            return dataBuffer;
        });

        MultipartBodyBuilder bodyBuilder = new MultipartBodyBuilder();
//        bodyBuilder.part("file", bufferFlux, DataBuffer.class)
        bodyBuilder.part("file", bufferFlux, MediaType.APPLICATION_OCTET_STREAM)
                .filename(file.getName())
                .header(HttpHeaders.CONTENT_DISPOSITION, "form-data; name=file; filename=" + file.getName());

        bodyBuilder.part("uploaderId", uploaderId.toString());
//        fileEndpoint.upload(bodyBuilder.build())

    }


    public ChatBubblePane(MessageWrapper messageWrapper) {
        this.sender = UserInfoContext.getCurrentUser().getUsername();
        var senderAccount = messageWrapper.getSenderAccount();
        // 判断是否为当前用户
        var isCurrentSender = sender.equals(senderAccount);
        init(messageWrapper, sender, isCurrentSender);
    }



    /**
     *
     * @param message message text
     * @param name  sender
     * @param isSent  isCurrentSender , if true , the avatar is on the right side
     */
    private void init(Object message, String name , boolean isSent) {
        this.setMaxWidth(Double.MAX_VALUE); // 允许外层VBox控制宽度
        HBox.setHgrow(this, Priority.ALWAYS); // 允许自己在HBox中扩展

        this.setSpacing(10);
        this.setPadding(new Insets(10));
        this.setAlignment(isSent ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);

        avatar = new ImageView(AvatarGenerator.generateCircleAvatar(name, 100));
        avatar.setFitWidth(40);
        avatar.setFitHeight(40);

        senderTextField = new RichTextMessageArea();
        senderTextField.setEditable(false);


        if(message instanceof String strContent){
            senderTextField.appendText(strContent);
        }
        // 如果直接是 消息节点那么直接添加即可
        else if (message instanceof MessageNode messageNode){
            senderTextField.insertNode(messageNode);
        }
        // 如果是 推送来的 MessageWrapper ，优先构造出属性 ，
        else if (message instanceof MessageWrapper messageWrapper){
            switch (messageWrapper.getMessageType()) {
                case TEXT -> senderTextField.appendText(messageWrapper.getContent());

                case FILE -> {
                     messageNodeService.createMessageNode(messageWrapper)
                            .subscribe(node -> senderTextField.insertNode(node));

                }

            }
        }


        senderTextField.setMaxWidth(400); // 限定最大宽度，控制文本换行
        senderTextField.setPrefHeight(Region.USE_COMPUTED_SIZE); // 允许自动高度
        VBox.setVgrow(senderTextField, Priority.ALWAYS);

        HBox messageBubble = new HBox(senderTextField);
        messageBubble.setPadding(new Insets(10));
        messageBubble.getStyleClass().add("message-bubble");
        messageBubble.setAlignment(Pos.TOP_LEFT); // 让文本从上往下布局
        messageBubble.setMaxWidth(400);
        messageBubble.setFillHeight(true); // 垂直拉伸
        HBox.setHgrow(senderTextField, Priority.ALWAYS);


        if (isSent) {
            messageBubble.getStyleClass().add("sent-message-bubble");
        }

        messageBubble.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(senderTextField, Priority.ALWAYS);  // 允许扩展

        VBox messageBox = new VBox(5);
        messageBox.getChildren().add(messageBubble);
        // 关键点：让 messageBox 占满宽度
        messageBox.setMaxWidth(Double.MAX_VALUE);
        VBox.setVgrow(messageBox, Priority.ALWAYS);
        VBox.setVgrow(messageBubble, Priority.ALWAYS); //

        senderTextField.setWrapText(true);  // 启用自动换行
        senderTextField.textProperty().addListener((obs, oldText, newText) -> {
            double newHeight = computeTextHeight(senderTextField);
            senderTextField.setPrefHeight(newHeight);
        });


        if (isSent) {
            this.getChildren().addAll(messageBox, avatar);
        } else {
            this.getChildren().addAll(avatar, messageBox);
        }
    }


    private ChatBubblePane(){

    }



    protected ChatBubblePane(Object message){

        var currentUser = UserInfoContext.getCurrentUser();

        this.sender = currentUser.getUsername();

        init(message, sender, true);

    }

    public ChatBubblePane(String sender, Object message) {
        if (sender == null || sender.isEmpty() || sender.equals(UserInfoContext.getCurrentUser().getUsername())){
            var currentUser = UserInfoContext.getCurrentUser();

            this.sender = currentUser.getUsername();

            init(message, sender, true);
        }

        else{
            this.sender = sender;
            init(message, sender, false);

        }
    }



    /**
     * 计算 InlineCssTextArea 需要的高度
     */
    private double computeTextHeight(GenericStyledArea textArea) {
        int lines = textArea.getParagraphs().size();
        double lineHeight = 20;
        return Math.max(40, lines * lineHeight + 10);
    }





}