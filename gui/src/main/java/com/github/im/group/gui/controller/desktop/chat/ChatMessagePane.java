package com.github.im.group.gui.controller.desktop.chat;

import com.github.im.common.connect.connection.client.ClientToolkit;
import com.github.im.common.connect.model.proto.BaseMessage;
import com.github.im.common.connect.model.proto.Chat;
import com.github.im.dto.session.MessageDTO;
import com.github.im.dto.user.UserInfo;
import com.github.im.enums.MessageType;
import com.github.im.group.gui.api.FileEndpoint;
import com.github.im.group.gui.api.MessageEndpoint;
import com.github.im.group.gui.connect.handler.EventBus;
import com.github.im.group.gui.context.UserInfoContext;
import com.github.im.group.gui.controller.desktop.chat.messagearea.richtext.RichTextMessageArea;
import com.github.im.group.gui.controller.desktop.chat.messagearea.richtext.FileResource;
import com.github.im.group.gui.controller.desktop.chat.messagearea.richtext.file.FileDisplay;
import com.github.im.group.gui.controller.desktop.chat.messagearea.richtext.file.LocalFileInfo;
import com.github.im.group.gui.util.ClipboardUtils;
import com.github.im.group.gui.util.ImageUtil;
import io.github.palexdev.materialfx.controls.MFXButton;
import io.github.palexdev.materialfx.controls.MFXScrollPane;
import io.github.palexdev.materialfx.enums.ButtonType;
import io.github.palexdev.materialfx.utils.ScrollUtils;
import jakarta.annotation.PostConstruct;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.image.Image;
import javafx.scene.input.Clipboard;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.reactfx.util.Either;
import org.springframework.context.annotation.Scope;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;

/**
 * Description: ChatMessagePane for every  conversation , every one is  independent.
 *
 * include following components:
 * <p>
 *     <ul>
 *         <li>{@link SendMessagePane} </li>
 *     </ul>
 * </p>
 *
 * @author pengpeng
 * @version 1.0
 * @since 2025/1/9
 */
@Component
@Scope("prototype")
@Slf4j
@RequiredArgsConstructor
public class ChatMessagePane extends BorderPane implements Initializable {


    @Getter
    @Setter
    private UserInfo toAccountInfo;

    @Getter
    @Setter
    private Long conversationId ;

    private VBox messageDisplayArea; // 消息展示区域

    private MFXScrollPane scrollPane; // 消息滚动条

    private SendMessagePane sendMessagePane;  // 消息发送区域

    private RichTextMessageArea messageSendArea; // message send area


    private final EventBus bus;
    private final MessageEndpoint messageEndpoint;
    private final FileEndpoint fileEndpoint;

    @Override
    public void initialize(URL location, ResourceBundle resources) {

    }


    /**
     * send message pane
     * contains
     * Button send
     */
    public static class SendMessagePane extends AnchorPane implements Initializable {


        private MFXButton sendButton;

        private SendMessagePane(){
            initialize();
        }


        /**
         *  set send message action
         * @param messageSendAction
         */
        private SendMessagePane(Mono<Void> messageSendAction){
            initialize();
            sendButton.setOnAction(event -> {
                messageSendAction.subscribe();
            });
        }



        @Override
        public void initialize(URL location, ResourceBundle resources) {

        }

        @PostConstruct
        public void initialize() {
            // 初始化  将文本域 放在 BorderPane 最上方
            sendButton = new MFXButton("发送");
            sendButton.setButtonType(ButtonType.RAISED);
            sendButton.setRippleColor(javafx.scene.paint.Color.DARKSEAGREEN);

            // 设置按钮的右下角位置
            AnchorPane.setBottomAnchor(sendButton, 10.0);  // 设置底部距 10 像素
            AnchorPane.setRightAnchor(sendButton, 10.0);   // 设置右侧距 10 像素


            this.getChildren().add(sendButton);
            this.setPrefHeight(50); // Set the height for the send area

        }

    }




    /**
     * 1. 订阅 EventBus，按 conversationId 过滤，串行处理每条消息
     */
    public Mono<Void> receiveChatMessageEvent() {
        return bus.asFlux()
                .ofType(Chat.ChatMessage.class)
                .filter(cm -> Objects.equals(cm.getConversationId(), getConversationId()))
                .concatMap(this::handleIncomingChatMessage)
                .then();
    }

    /**
     * 2. 路由到不同类型处理
     */
    private Mono<Void> handleIncomingChatMessage(Chat.ChatMessage cm) {
        String sender = cm.getFromAccountInfo().getAccount();
        switch (cm.getType()) {
            case TEXT:
                return showTextBubble(sender, cm.getContent());
            case STREAM:
            case VIDEO:
            case IMAGE:
            case FILE:
                return showResourceBubble(sender, cm.getContent());
            default:
                return Mono.empty();  // 未知类型忽略
        }
    }

    /**
     * 3a. 文本消息：直接在 UI 线程添加气泡
     */
    private Mono<Void> showTextBubble(String sender, String text) {
        return Mono.fromRunnable(() ->
                Platform.runLater(() -> {
                    addMessageBubble(sender, text);
                    scrollPane.setVvalue(1.0);
                })
        );
    }

    /**
     * 3b. 资源消息（图片/视频）：下载 → 转 Image → UI 显示 → 本地保存
     */
    private Mono<Void> showResourceBubble(String sender, String fileId) {
        UUID id = UUID.fromString(fileId);

        return fileEndpoint.downloadFile(id)                      // Mono<ResponseEntity<Resource>>
                .flatMap(response -> {
                    Resource res = response.getBody();
                    if (res == null) return Mono.empty();

                    // 3b-1. 取文件名、类型、长度
                    HttpHeaders headers = response.getHeaders();
                    String filename = Optional.ofNullable(headers.getContentDisposition())
                            .map(ContentDisposition::getFilename)
                            .orElse(id.toString());
                    MediaType mime = headers.getContentType();
                    long length = headers.getContentLength();

                    // 3b-2. 异步转为 JavaFX Image
                    Mono<Image> imageMono = Mono.fromCallable(() -> {
                        byte[] bytes = StreamUtils.copyToByteArray(res.getInputStream());
                        return ImageUtil.bytesToImage(bytes);
                    }).subscribeOn(Schedulers.boundedElastic());

                    // 3b-3. UI 显示气泡
                    Mono<Void> uiMono = imageMono.doOnNext(img ->
                            Platform.runLater(() -> {
                                addMessageBubble(sender, img);
                                scrollPane.setVvalue(1.0);
                            })
                    ).then();

                    // 3b-4. 后台写盘
                    Mono<Void> saveMono = Mono.fromRunnable(() -> {
                        Path target = Paths.get("downloads", filename);
                        try {
                            Files.createDirectories(target.getParent());
                            try (InputStream in = res.getInputStream()) {
                                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
                            }
                        } catch (IOException ex) {
                            log.warn("下载文件保存失败: {}", filename, ex);
                        }
                    }).subscribeOn(Schedulers.boundedElastic()).then();

                    // 3b-5. 串联显示与保存
                    return uiMono.then(saveMono);
                })
                .onErrorResume(err -> {
                    log.error("资源消息处理失败 fileId=" + fileId, err);
                    return Mono.empty();
                });
    }
    /**
     * 添加消息
     * 此方法用于在用户界面上添加新的消息气泡它首先检查消息的发送者是否是当前用户，
     * 然后根据结果决定是显示在 左边（其他用户）还是右边（当前用户）
     *
     * @param messageDTO 包含消息详细信息的数据传输对象
     */
    public void addMessage(MessageDTO messageDTO) {
        handleMessageDTO(messageDTO)
                .subscribe();  // fire-and-forget
    }

    private Mono<Void> handleMessageDTO(MessageDTO dto) {
//        String me = UserInfoContext.getCurrentUser().getUserId().toString();
        String sender = dto.getFromAccount().getUsername();
        var content = dto.getContent();
        if (dto.getType() == MessageType.TEXT) {
            String text = content;
            return showTextBubble(sender , text);
        } else {
            // 资源消息同上
            return showResourceBubble(sender,content);
        }
    }

    public void addMessages(List<MessageDTO> messageDTOs){
        Platform.runLater(()->{
            messageDTOs.stream()
                    .sorted((e1,e2)-> {
                        if(e1.getSequenceId() != null && e2.getSequenceId() != null){
                           return e1.getSequenceId().compareTo(e2.getSequenceId());
                        }else {
                            return 0;
                        }
                    }) // 根据服务端的消息保障时序性
                    .forEach(dto->addMessage(dto));
        });
    }

    private Mono<List<Either<String, FileResource>>> collectSegments() {
        var doc = messageSendArea.getDocument();
        List<Either<String, FileResource>> segments = new ArrayList<>();
        doc.getParagraphs().forEach(par -> par.getSegments().forEach(segments::add));
        if (segments.isEmpty()) {
            return Mono.error(new IllegalArgumentException("Message cannot be blank"));
        }
        return Mono.just(segments);
    }
    private Flux<Void> sendSegmentsSequentially(List<Either<String, FileResource>> segments) {
        return Flux.fromIterable(segments)
                .concatMap(this::sendSegment);
    }
    private Mono<Void> sendSegment(Either<String, FileResource> segment) {
        return segment.isLeft() ? sendTextSegment(segment.getLeft())
                : sendImageSegment(segment.getRight());
    }

    private Mono<Void> sendTextSegment(String text) {
        if (text.isBlank()) return Mono.empty();
        var msg = Chat.ChatMessage.newBuilder()
                .setConversationId(getConversationId())
                .setFromAccountInfo(UserInfoContext.getAccountInfo())
                .setType(Chat.MessageType.TEXT)
                .setContent(text)
                .build();

        var pkg = BaseMessage.BaseMessagePkg.newBuilder().setMessage(msg).build();

        return ClientToolkit.reactiveClientAction()
                .sendMessage(pkg)
                .doOnTerminate(() -> Platform.runLater(() -> {
                    addMessageBubble(text);
                    scrollPane.setVvalue(1.0);
                }))
                .then();
    }
    private Mono<Void> sendImageSegment(FileResource img) {
        var uuid = UUID.randomUUID();
        ByteArrayResource resource = new ByteArrayResource(img.getBytes()) {
            @Override public String getFilename() { return "image.jpg"; }
        };

        return fileEndpoint.upload(resource, uuid)
                .flatMap(resp -> {
                    var msg = Chat.ChatMessage.newBuilder()
                            .setConversationId(getConversationId())
                            .setFromAccountInfo(UserInfoContext.getAccountInfo())
                            .setType(img.isReal() ? Chat.MessageType.FILE : Chat.MessageType.STREAM)
                            .setContent(resp.getId().toString())
                            .build();
                    var pkg = BaseMessage.BaseMessagePkg.newBuilder().setMessage(msg).build();

                    return ClientToolkit.reactiveClientAction()
                            .sendMessage(pkg)
                            .doOnTerminate(() -> Platform.runLater(() -> {
                                addMessageBubble(img.getImage());
                                scrollPane.setVvalue(1.0);
                            }))
                            .then();
                });
    }






    /**
     * 发送消息
     * 此处需要作 消息的拆分发送；
     * 当消息是富文本时 ，需要将 文件、 图片、 视频等资源与文字进行拆分，然后分别发送给服务器。
     */
    private Mono<Void> sendMessage() {
        return Mono.defer(this::collectSegments)
                .flatMapMany(this::sendSegmentsSequentially)
                .then()
                .doOnSuccess(v -> Platform.runLater(messageSendArea::clear))
                .onErrorResume(e -> {
                    log.error("Failed to send rich message: {}", e.getMessage(), e);
                    return Mono.empty();
                });
    }



    /**
     * Adds a new message bubble to the display area on the JavaFX Application Thread.
     *
     * @param message            The message content
     */
    private  void addMessageBubble(Object message) {
        Platform.runLater(() -> messageDisplayArea.getChildren().add(new ChatBubblePane(message)));
    }

    /**
     * Adds a new message bubble to the display area on the JavaFX Application Thread.
     * @param sender message sender
     * @param message  message content
     */
    private void addMessageBubble( String sender , Object message) {
        Platform.runLater(() -> messageDisplayArea.getChildren().add(new ChatBubblePane(sender,message)));
    }


    @PostConstruct
    public void initialize() {
        // 监听聊天消息事件
        receiveChatMessageEvent()
                .subscribeOn(Schedulers.boundedElastic())
                .subscribe(
                        null,
                        err -> log.error("消息接收流程异常", err)
                );;


        // Initialize message display area
        messageDisplayArea = new VBox(10);
        messageDisplayArea.setPadding(new Insets(10)); // 设置内边距


        // Initialize send message area
        messageSendArea = new RichTextMessageArea();

        // 粘贴监听
        messageSendArea.setOnKeyReleased(event -> {
            if (event.isControlDown() && event.getCode() == KeyCode.V) {
                var imageFromClipboard = ClipboardUtils.getImageFromClipboard();
                if (imageFromClipboard != null){
                    messageSendArea.insertImage(imageFromClipboard); // 插入图片
                    event.consume(); // 阻止默认粘贴行为（可选）
                }

                var filesFromClipboard = ClipboardUtils.getFilesFromClipboard();
                final var containsFile = !filesFromClipboard.isEmpty();  //存在文件
                if (containsFile){
                    filesFromClipboard.stream().forEach(file -> {
                        if (file.isFile()){
                            var localFileInfo = new LocalFileInfo(file.toPath());
                            var fileDisplay = new FileDisplay(localFileInfo);
                            messageSendArea.insertFile(fileDisplay); // 插入文件
                        }
                    });


                }
            }
        });

        // Create a scroll pane for message display area
        scrollPane = new MFXScrollPane(messageDisplayArea);
        scrollPane.setFitToWidth(true);
        ScrollUtils.addSmoothScrolling(scrollPane);


        scrollPane.setPrefHeight(300);

        messageSendArea.setPrefHeight(200); // 设置组件的最小高度

        // Create a SendMessagePane instance and place it in the bottom-right corner

        sendMessagePane = new SendMessagePane(sendMessage());
        sendMessagePane.setPrefHeight(50);

        sendMessagePane.prefHeightProperty().bind(Bindings.multiply(this.heightProperty(), 0.1));

        // Vbox 每次变动都会滚动到最底部
        messageDisplayArea.heightProperty().addListener((observable, oldValue, newValue) -> {
            scrollPane.setVvalue(1.0); // Scroll to the bottom
        });

        // 回车触发发送事件
        messageSendArea.setOnKeyPressed(event -> {
            switch (event.getCode()) {
                case ENTER -> {
                    if (event.isShiftDown()) {
                        // 允许 Shift + Enter 插入换行
                        messageSendArea.insertText(messageSendArea.getCaretPosition(), "\n");
                    } else {
                        event.consume(); // 阻止默认回车行为
                        sendMessage().subscribe();
                    }

                }
            }
        });



        this.setTop(scrollPane);
        this.setCenter(messageSendArea);
        this.setBottom(sendMessagePane);

    }


}