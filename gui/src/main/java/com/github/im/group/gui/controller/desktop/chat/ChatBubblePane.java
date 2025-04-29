package com.github.im.group.gui.controller.desktop.chat;

import com.github.im.dto.file.FileUploadResponse;
import com.github.im.group.gui.api.FileEndpoint;
import com.github.im.group.gui.context.UserInfoContext;
import com.github.im.group.gui.controller.desktop.chat.messagearea.richtext.RichTextMessageArea;
import com.github.im.group.gui.util.AvatarGenerator;
import com.jfoenix.controls.JFXTextArea;
import io.github.palexdev.materialfx.controls.MFXTextField;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
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
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
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
public class ChatBubblePane extends HBox {


    private String sender;

    private String message;

    // 创建头像
    private ImageView avatar ;

    private Label senderLabel;

    private RichTextMessageArea senderTextField;

    private ProgressBar progressBar;

    private Label statusLabel;

    private FileEndpoint fileEndpoint;

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

    /**
     *
     * @param message message text
     * @param name  sender
     * @param isSent  isCurrentSender , if true , the avatar is on the right side
     */
    private void init(Object message, String name , boolean isSent) {

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
        else if(message instanceof Image imageContent){
            senderTextField.insertImage(imageContent);
        }
        senderTextField.setWrapText(true);
        senderTextField.setMaxWidth(250);


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
    private double computeTextHeight(InlineCssTextArea textArea) {
        int lines = textArea.getText().split("\n").length;  // 计算文本行数
        double lineHeight = 18; // 估算单行高度（可根据字体调整）
        return Math.max(40, lines * lineHeight + 10); // 最小高度 40
    }





}