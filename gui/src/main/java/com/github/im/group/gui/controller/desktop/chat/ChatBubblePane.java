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
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.fxmisc.richtext.GenericStyledArea;
import org.springframework.beans.factory.annotation.Autowired;
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
import reactor.core.publisher.Mono;

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
//public class ChatBubblePane extends HBox {
public class ChatBubblePane extends GridPane {


    private String sender;

    // 创建头像
    private ImageView avatar ;

    private Label senderLabel;

    private RichTextMessageArea senderTextField;

    private ProgressBar progressBar;

    private Label statusLabel;

    private MessageWrapper messageWrapper;  // 消息主体

    @Setter
    /**
     * 返回一个消息 Node
     */
    private Mono<MessageNode> messageNodeMono;


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


    public ChatBubblePane(MessageWrapper messageWrapper,Mono<MessageNode> nodeMono) {
        this.messageNodeMono = nodeMono;
        this.messageWrapper = messageWrapper;
        this.sender = UserInfoContext.getCurrentUser().getUsername();
        var senderAccount = messageWrapper.getSenderAccount();
        // 判断是否为当前用户
        var isCurrentSender = sender.equals(senderAccount);
        init(messageWrapper, sender, isCurrentSender);
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
//        this.setMaxWidth(Double.MAX_VALUE); // 允许外层VBox控制宽度
//        HBox.setHgrow(this, Priority.ALWAYS); // 允许自己在HBox中扩展
        // 背景 黑色
        this.setBackground(new Background(new BackgroundFill(javafx.scene.paint.Color.BLACK, null, null)));
//        this.setSpacing(10);
//        this.setPadding(new Insets(10));
//        this.setAlignment(isSent ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
        this.setHgap(10);
        this.setMaxWidth(Double.MAX_VALUE);
        this.setPrefWidth(400); // 控制最大宽度
        // 构造空白填充列
        var columnConstraints = new ColumnConstraints();
//        columnConstraints.setPrefWidth(40);
        columnConstraints.setPercentWidth(20);
        // 构造消息展示列 ，宽度等于 剩余宽度

        var messageColumn = new ColumnConstraints();
//        messageColumn.setPercentWidth().bind(this.widthProperty().subtract(columnConstraints.getPrefWidth()));
        messageColumn.setPercentWidth(80);
//                .bind(this.widthProperty().subtract(columnConstraints.getPrefWidth()));
//        columnConstraints.setPrefWidth(40);




        avatar = new ImageView(AvatarGenerator.generateCircleAvatar(name, 100));
        avatar.setFitWidth(40);
        avatar.setFitHeight(40);

        senderTextField = new RichTextMessageArea();
        senderTextField.setEditable(false);

        senderTextField.setBackground(new Background(new BackgroundFill(Color.BLUE, null, null)));

        if(message instanceof String strContent){
            senderTextField.appendText(strContent);
        }
        // 如果直接是 消息节点那么直接添加即可
        else if (message instanceof MessageNode messageNode){
            senderTextField.insertNode(messageNode);
        }
        // 如果是 推送来的 MessageWrapper ，优先构造出属性 ，
        else if (message instanceof MessageWrapper messageWrapper){
            this.messageWrapper = messageWrapper;
            switch (messageWrapper.getMessageType()) {
                case TEXT -> senderTextField.appendText(messageWrapper.getContent());
                case FILE -> {
                    messageNodeMono
                            .subscribe(node -> senderTextField.insertNode(node));
                }

            }
        }


//        senderTextField.setMaxWidth(400); // 限定最大宽度，控制文本换行
//        senderTextField.setPrefHeight(Region.USE_COMPUTED_SIZE); // 允许自动高度
//        VBox.setVgrow(senderTextField, Priority.ALWAYS);

//        HBox messageBubble = new HBox(senderTextField);
//        messageBubble.setPadding(new Insets(5));
//        messageBubble.getStyleClass().add("message-bubble");
//        messageBubble.setAlignment(Pos.TOP_LEFT); // 让文本从上往下布局
//        messageBubble.setFillHeight(true); // 垂直拉伸
//        messageBubble.setBackground(new Background(new BackgroundFill(Color.RED, null, null)));

//        HBox.setHgrow(senderTextField, Priority.ALWAYS);


//        if (isSent) {
//            messageBubble.getStyleClass().add("sent-message-bubble");
//        }
//        HBox.setHgrow(senderTextField, Priority.ALWAYS);  // 允许扩展

        HBox messageBox = new HBox(10);
//        messageBox.set
        messageBox.setBackground(new Background(new BackgroundFill(Color.GREEN, null, null)));
        messageBox.setAlignment(isSent ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
//        messageBox.setHgrow(senderTextField, Priority.ALWAYS);
        senderTextField.setPrefWidth(170);
        if(isSent){
            messageBox.getChildren().addAll(senderTextField,avatar);
        }
        else{
            messageBox.getChildren().addAll(avatar, senderTextField);
        }
//        VBox.setVgrow(senderTextField, Priority.ALWAYS);              // 允许 VBox 分配多余高度
//        senderTextField.setMaxHeight(Double.MAX_VALUE);               // 控件自己也允许拉伸
//        senderTextField.setMaxWidth(Double.MAX_VALUE);                // 宽度也要允许拉伸
//        messageBox.getChildren().addAll(senderTextField, avatar);

//        var button = new Button("1111");
//        button.setBackground(new Background(new BackgroundFill(Color.BLUE, null, null)));
//
//        messageBox.getChildren().add(button);
//        HBox.setHgrow(button, Priority.ALWAYS);

//        VBox.set(button, Priority.ALWAYS);              // 允许 VBox 分配多余高度

//        button.setMaxHeight(Double.MAX_VALUE);               // 控件自己也允许拉伸
//        button.setMaxWidth(Double.MAX_VALUE);                // 宽度也要允许拉伸
//        // 关键点：让 messageBox 占满宽度
//
//        messageBox.setMaxWidth(Double.MAX_VALUE);
//
//        messageBox.setBackground(new Background(new BackgroundFill(Color.GREEN, null, null)));
//
//        VBox.setVgrow(messageBox, Priority.ALWAYS);
//        VBox.setVgrow(messageBubble, Priority.ALWAYS); //

//        senderTextField.setWrapText(true);  // 启用自动换行
//        senderTextField.textProperty().addListener((obs, oldText, newText) -> {
//            double newHeight = computeTextHeight(senderTextField);
//            senderTextField.setPrefHeight(newHeight);
//        });
//        senderTextField.setPrefWidth(150);
//        senderTextField.setMaxWidth(180);
//        var region = new Region();
//        region.setPrefWidth(10);
//        region.setMaxWidth(10);
        // 绿色
//        region.setBackground(new Background(new BackgroundFill(Color.GREEN, null, null)));

//        HBox.setHgrow(region, Priority.ALWAYS);
//        this.getChildren().add(region);
        if (isSent) {
            this.getColumnConstraints().add(columnConstraints);
            this.getColumnConstraints().add(messageColumn);

//            this.add(senderTextField,1,0);
            this.add(messageBox,1,0);
//            this.getChildren().addAll(messageBox, avatar);
//            this.getChildren().addAll(messageBubble, avatar);
//            this.getChildren().addAll(senderTextField, avatar);
        } else {
            this.getColumnConstraints().add(messageColumn);
            this.getColumnConstraints().add(columnConstraints);

//            this.getChildren().addAll(avatar, messageBox);
            this.add(messageBox,0,0);
//            this.add(senderTextField,0,0);
//            this.getChildren().addAll(avatar, messageBubble);
//            this.getChildren().addAll(avatar, senderTextField);
        }
        System.out.println("line  length" + senderTextField.getPrefWidth());
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



//    /**
//     * 计算 InlineCssTextArea 需要的高度
//     */
//    private double computeTextHeight(GenericStyledArea textArea) {
//        int lines = textArea.getParagraphs().size();
//        double lineHeight = 20;
//        return Math.max(40, lines * lineHeight + 10);
//    }



}