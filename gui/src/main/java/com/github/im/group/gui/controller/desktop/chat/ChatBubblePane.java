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



    private UserInfo senderUser;

    // 创建头像
    private final Avatar avatar ;

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

        UserInfo currentUser = UserInfoContext.getCurrentUser();
        var senderAccount = messageWrapper.getSenderAccount();
        senderUser = messageWrapper.getUserInfo();
        // 判断是否为当前用户
        var isCurrentSender = currentUser.getUsername().equals(senderAccount);
        var username = currentUser.getUsername();

        avatar = AvatarGenerator.getAvatar(username, 20);

        init(messageWrapper, username, isCurrentSender);
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
            this.messageWrapper = messageWrapper;
            switch (messageWrapper.getMessageType()) {
                case TEXT -> senderTextField.appendText(messageWrapper.getContent());
                case FILE -> {
                    messageNodeMono
                            .subscribe(node -> senderTextField.insertNode(node));
                }

            }
        }

        // 头像和名称的Box
        var avatarAndNameBox = new VBox(5);
        senderLabel = new Label(name);
        senderLabel.setFont(Font.font("Arial", 18));
        senderLabel.setTextFill(Color.GRAY); // 设置为标准灰色

        avatarAndNameBox.getChildren().addAll(senderLabel, avatar);
        avatarAndNameBox.setAlignment(isSent ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);

        HBox messageBox = new HBox(10);
        messageBox.setAlignment(isSent ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);

//        TODO 抽象成主题色
        var instance = AppManager.getInstance();
        Background background;
        BackgroundFill backgroundFill;
        if (instance != null){
            var fill = instance.getAppBar().getBackground().getFills().get(0).getFill();
            backgroundFill = new BackgroundFill(
                    fill, // 蓝色
                    new CornerRadii(15),  // 圆角半径
                    Insets.EMPTY
            );
        }else{
             backgroundFill = new BackgroundFill(
                    Color.BLUE, // 蓝色
                    new CornerRadii(15),  // 圆角半径
                    Insets.EMPTY
            );
        }

        background = new Background(backgroundFill);
        var senderTextFieldBox = new HBox();
        senderTextFieldBox.getChildren().add(senderTextField);
        senderTextFieldBox.setPadding(new Insets(10,0,10,0));

        if(isSent){
            senderTextField.setBackground(background);
            messageBox.getChildren().addAll(senderTextFieldBox,avatarAndNameBox);
        }
        else{
            messageBox.getChildren().addAll(avatarAndNameBox, senderTextFieldBox);
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

//        senderTextField.setPrefSize();
        senderTextField.setPrefWidth(400);
        senderTextField.setPrefHeight(senderTextField.calculateAreaHeight(400L));

       log.info("line  length {} , {} " ,senderTextField.getPrefWidth() , senderTextField.getPrefHeight());
    }



}