package com.github.im.group.gui.controller.desktop.chat;

import com.github.im.dto.user.UserInfo;
import com.github.im.group.gui.context.UserInfoContext;
import com.github.im.group.gui.controller.desktop.MessageWrapper;
import com.github.im.group.gui.controller.desktop.chat.messagearea.MessageNodeService;
import com.github.im.group.gui.controller.desktop.chat.messagearea.richtext.MessageNode;
import com.github.im.group.gui.controller.desktop.chat.messagearea.richtext.RichTextMessageArea;
import com.github.im.group.gui.controller.desktop.chat.messagearea.richtext.file.FileNode;
import com.github.im.group.gui.controller.desktop.chat.messagearea.richtext.file.RemoteFileInfo;
import com.github.im.group.gui.util.AvatarGenerator;
import com.gluonhq.charm.glisten.application.AppManager;
import com.gluonhq.charm.glisten.control.Avatar;
import com.sun.javafx.tk.Toolkit;
import jakarta.annotation.Resource;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.richtext.GenericStyledArea;
import org.fxmisc.richtext.model.Paragraph;
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
@Slf4j
public class ChatBubblePane extends GridPane {


    // 创建头像
    private final Avatar avatar ;
    // 消息发送者
    private Label senderLabel;
    // 文本内容
    private RichTextMessageArea messageArea;
    // 进度条
    private ProgressBar progressBar;

    private final MessageWrapper messageWrapper;  // 消息主体

    @Setter
    /**
     * 返回一个消息 Node {@link MessageNode 用于展示的消息节点 }
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
//                statusLabel.setText(String.format("上传中：%.2f%%", progress * 100));
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


    /**
     *
     * @param messageWrapper 消息包装体
     * @param nodeMono
     */
    public ChatBubblePane(MessageWrapper messageWrapper,Mono<MessageNode> nodeMono) {
        this.messageNodeMono = nodeMono;
        this.messageWrapper = messageWrapper;

        UserInfo currentUser = UserInfoContext.getCurrentUser();
        var senderAccount = messageWrapper.getSenderAccount();
        // 判断是否为当前用户
        var isCurrentSender = currentUser.getUsername().equals(senderAccount);

        avatar = AvatarGenerator.getAvatar(senderAccount, AvatarGenerator.AvatarSize.MEDIUM);

        init(messageWrapper, senderAccount, isCurrentSender);
    }



    /**
     *
     * @param message message text
     * @param name  sender
     * @param isSent  isCurrentSender , if true , the avatar is on the right side
     */
    private void init(Object message, String name , boolean isSent) {
        this.setHgap(10);
        this.setMaxWidth(Double.MAX_VALUE);
        this.setPrefWidth(400); // 控制最大宽度
        final var paddingTopAndBottom  = 10;
        this.setPadding(new Insets(paddingTopAndBottom,0,paddingTopAndBottom,0));


        // 构造空白填充列
        var columnConstraints = new ColumnConstraints();
        columnConstraints.setPercentWidth(20);
        // 构造消息展示列 ，宽度等于 剩余宽度

        var messageColumn = new ColumnConstraints();
        messageColumn.setPercentWidth(80);


        messageArea = new RichTextMessageArea();
        messageArea.setEditable(false);

        if(message instanceof String strContent){
            messageArea.appendText(strContent);
        }
        // 如果直接是 消息节点那么直接添加即可
        else if (message instanceof MessageNode messageNode){
            messageArea.insertNode(messageNode);
        }
        // 如果是 推送来的 MessageWrapper ，优先构造出属性 ，
        else if (message instanceof MessageWrapper wrapper){
            switch (messageWrapper.getMessageType()) {
                case TEXT -> messageArea.appendText(messageWrapper.getContent());
                case FILE -> {
                    messageNodeMono
                            .subscribe(node -> messageArea.insertNode(node));
                }

            }
        }

        // 头像和名称的Box
        var avatarAndNameBox = new VBox(5);
        senderLabel = new Label(name);
        senderLabel.setFont(Font.font("Arial", 10));
        senderLabel.setTextFill(Color.GRAY); // 设置为标准灰色

        avatarAndNameBox.getChildren().addAll(senderLabel, avatar);
        avatarAndNameBox.setAlignment(isSent ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);

        // 存放消息文本 + 头像Box 独占一行
        HBox messageBox = new HBox(10);
        messageBox.setAlignment(isSent ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);


        // 添加一些上下边距 不让内容 贴太近
        var messageAreaBox = new HBox();
        messageAreaBox.setPadding(new Insets(10,0,10,0));

        messageAreaBox.getChildren().add(messageArea);

        VBox.setMargin(messageAreaBox, new Insets(10, 0, 10, 0));

        if(isSent){
            messageArea.bg();
            messageBox.getChildren().addAll(messageAreaBox,avatarAndNameBox);
        }
        else{
            messageBox.getChildren().addAll(avatarAndNameBox, messageAreaBox);
        }

        if (isSent) {
            this.getColumnConstraints().add(columnConstraints);
            this.getColumnConstraints().add(messageColumn);

            this.add(messageBox,1,0);
        } else {
            this.getColumnConstraints().add(messageColumn);
            this.getColumnConstraints().add(columnConstraints);

            this.add(messageBox,0,0);
        }

//        messageArea.setPrefSize();
        messageArea.setPrefWidth(200);
        messageArea.setPrefHeight(messageArea.calculateAreaHeight(400L));

       log.info("line  length {} , {} " ,messageArea.getPrefWidth() , messageArea.getPrefHeight());
    }



}