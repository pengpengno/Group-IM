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
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
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
public class ChatBubblePane extends GridPane {



    private UserInfo senderUser;

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

        UserInfo currentUser = UserInfoContext.getCurrentUser();
        var senderAccount = messageWrapper.getSenderAccount();
        senderUser = messageWrapper.getUserInfo();
        // 判断是否为当前用户
        var isCurrentSender = currentUser.getUsername().equals(senderAccount);
        init(messageWrapper, currentUser.getUsername(), isCurrentSender);
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
        // 构造空白填充列
        var columnConstraints = new ColumnConstraints();
        columnConstraints.setPercentWidth(20);
        // 构造消息展示列 ，宽度等于 剩余宽度

        var messageColumn = new ColumnConstraints();
        messageColumn.setPercentWidth(80);

        avatar = new ImageView(AvatarGenerator.generateCircleAvatar(name, 100));
        avatar.setFitWidth(40);
        avatar.setFitHeight(40);

        senderTextField = new RichTextMessageArea();
        senderTextField.setEditable(false);

//        senderTextField.setBackground(new Background(new BackgroundFill(Color.BLUE, null, null)));

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
        senderLabel.setFont(Font.font("Arial", 15));

        avatarAndNameBox.getChildren().addAll(senderLabel, avatar);
        avatarAndNameBox.setAlignment(isSent ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);

        HBox messageBox = new HBox(10);
        messageBox.setAlignment(isSent ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);

        if(isSent){
            messageBox.getChildren().addAll(senderTextField,avatarAndNameBox);
        }
        else{
            messageBox.getChildren().addAll(avatarAndNameBox, senderTextField);
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

        senderTextField.setPrefWidth(400);
        senderTextField.setPrefHeight(senderTextField.computePrefHeight());

        System.out.println("line  length" + senderTextField.getPrefWidth());
    }

    /**
     * 计算 InlineCssTextArea 或 GenericStyledArea 所需的高度（粗略估算）。
     */
    private double computeTextHeight(RichTextMessageArea textArea) {
        // 1. 获取字体行高（从样式或默认字体）
        Font font = RichTextMessageArea.getFont();
        double lineHeight = Toolkit.getToolkit().getFontLoader().getFontMetrics(font).getLineHeight();

        // 2. 获取当前行数
        int paragraphCount = textArea.getParagraphs().size();

        // 3. 考虑每段文本的换行（折行）——可以通过内容长度和宽度估算
        double width = textArea.getWidth() > 0 ? textArea.getWidth() : 400; // 默认宽度防止为 0
        int wrapLines = 0;
        for (Paragraph<?, ?, ?> paragraph : textArea.getParagraphs()) {
            String text = paragraph.getText();
            Text helper = new Text(text);
            helper.setFont(font);
            helper.setWrappingWidth(width);
            new Scene(new Group(helper)); // 必须附加到 Scene 才能计算布局
            helper.applyCss();
            double paraHeight = helper.getLayoutBounds().getHeight();
            wrapLines += Math.max(1, (int) Math.ceil(paraHeight / lineHeight));
        }

        // 4. 总高度 = 行数 * 行高 + padding
        double totalHeight = wrapLines * lineHeight + 10; // 加一点 padding

        // 5. 设置最大高度限制（如不超过 300px）
        double maxHeight = 300;
        return Math.min(totalHeight, maxHeight);
    }


    /**
     * 计算 InlineCssTextArea 或 GenericStyledArea 所需的高度（粗略估算）。
     * 支持自动根据内容换行，设置最小最大高度。
     */
    private double computeTextHeight(RichTextMessageArea textArea, double maxWidth, double minHeight, double maxHeight) {
        Font font = RichTextMessageArea.getFont();
        double lineHeight = Toolkit.getToolkit()
                .getFontLoader()
                .getFontMetrics(font)
                .getLineHeight();

        double width = textArea.getWidth() > 0 ? textArea.getWidth() : maxWidth;

        Text helper = new Text();
        helper.setFont(font);
        helper.setWrappingWidth(width);
        new Scene(new Group(helper)); // 必须放入 Scene 才能正确计算
        helper.applyCss();

        int wrapLines = 0;
        for (Paragraph<?, ?, ?> paragraph : textArea.getParagraphs()) {
            String text = paragraph.getText();
            if (text.isEmpty()) {
                wrapLines += 1;
                continue;
            }

            helper.setText(text);
            double paraHeight = helper.getLayoutBounds().getHeight();
            wrapLines += Math.max(1, (int) Math.ceil(paraHeight / lineHeight));
        }

        double totalHeight = wrapLines * lineHeight + 10; // padding 可调整
        return Math.max(minHeight, Math.min(totalHeight, maxHeight));
    }




}