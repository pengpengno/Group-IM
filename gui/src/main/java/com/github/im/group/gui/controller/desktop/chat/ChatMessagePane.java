package com.github.im.group.gui.controller.desktop.chat;

import com.github.im.common.connect.connection.client.ClientToolkit;
import com.github.im.common.connect.model.proto.BaseMessage;
import com.github.im.common.connect.model.proto.Chat;
import com.github.im.dto.session.MessageDTO;
import com.github.im.dto.session.MessagePayLoad;
import com.github.im.dto.user.UserInfo;
import com.github.im.group.gui.api.FileEndpoint;
import com.github.im.group.gui.api.MessageEndpoint;
import com.github.im.group.gui.connect.handler.EventBus;
import com.github.im.group.gui.context.UserInfoContext;
import com.github.im.group.gui.controller.desktop.MessageWrapper;
import com.github.im.group.gui.controller.desktop.chat.messagearea.MessageNodeService;
import com.github.im.group.gui.controller.desktop.chat.messagearea.richtext.RichTextMessageArea;
import com.github.im.group.gui.controller.desktop.chat.messagearea.richtext.MessageNode;
import com.github.im.group.gui.controller.desktop.chat.messagearea.richtext.file.FileNode;
import com.github.im.group.gui.controller.desktop.chat.messagearea.richtext.file.LocalFileInfo;
import com.github.im.group.gui.controller.desktop.chat.messagearea.richtext.file.RemoteFileInfo;
import com.github.im.group.gui.controller.desktop.chat.messagearea.richtext.file.RemoteFileService;
import com.github.im.group.gui.controller.desktop.chat.messagearea.richtext.image.StreamImage;
import com.github.im.group.gui.controller.desktop.menu.impl.ChatButton;
import com.github.im.group.gui.util.ClipboardUtils;
import com.github.im.group.gui.util.PathFileUtil;
import io.github.palexdev.materialfx.controls.MFXButton;
import io.github.palexdev.materialfx.controls.MFXScrollPane;
import io.github.palexdev.materialfx.enums.ButtonType;
import io.github.palexdev.materialfx.utils.ScrollUtils;
import jakarta.annotation.PostConstruct;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.reactfx.util.Either;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Scope;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyExtractors;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.File;
import java.net.URL;
import java.nio.file.*;
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
public class ChatMessagePane extends BorderPane implements Initializable  {


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

    private final FileEndpoint fileEndpoint;
    private final MessageNodeService messageNodeService;

    private final ApplicationContext applicationContext;


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

        log.debug("receive message from {}",cm.getFromAccountInfo().getAccount());


        return Mono.fromSupplier(() -> {
            log.debug("receive message from {}",cm.getFromAccountInfo().getAccount());
            String sender = cm.getFromAccountInfo().getAccount();

            var messageWrapper = new MessageWrapper(cm);
            return messageWrapper;
        })
        .flatMap(messageWrapper -> {
//            addMessageBubble(messageWrapper);
            return Mono.fromRunnable(()-> {
                addMessageBubble(messageWrapper);
                //TODO
                scrollPane.setVvalue(1.0);
            });
        })
        ;



    }

    /**
     * 添加消息
     * 此方法用于在用户界面上添加新的消息气泡它首先检查消息的发送者是否是当前用户，
     * 然后根据结果决定是显示在 左边（其他用户）还是右边（当前用户）
     *
     * @param messageDTO 包含消息详细信息的数据传输对象
     */
    public void addMessage(MessageDTO<MessagePayLoad> messageDTO) {
        handleMessageDTO(messageDTO)
                .subscribe();  // fire-and-forget
    }

    private Mono<Void> handleMessageDTO(MessageDTO<MessagePayLoad> dto) {


        return Mono.fromSupplier(() -> {
                    log.debug("receive message from {}",dto);
                    return new MessageWrapper(dto);
                })
                .flatMap(messageWrapper -> Mono.fromRunnable(()-> {
                    addMessageBubble(messageWrapper);
                    scrollPane.setVvalue(1.0);
                }))
                ;
    }

    public void addMessages(List<MessageDTO<MessagePayLoad>> messageDTOs){
        Platform.runLater(()->{
            messageDTOs.stream()
                    .sorted((e1,e2)-> {
                        if(e1.getSequenceId() != null && e2.getSequenceId() != null){
                           return e1.getSequenceId().compareTo(e2.getSequenceId());
                        }else {
                            return 0;
                        }
                    }) // 根据服务端的消息保障时序性
                    .forEach(this::addMessage);
        });
    }


    private Mono<List<Either<String, MessageNode>>> collectSegments() {
        var doc = messageSendArea.getDocument();
        List<Either<String, MessageNode>> segments = new ArrayList<>();
        doc.getParagraphs().forEach(par -> segments.addAll(par.getSegments()));
        if (segments.isEmpty()) {
            return Mono.error(new IllegalArgumentException("Message cannot be blank"));
        }
        return Mono.just(segments);
    }
    private Flux<Void> sendSegmentsSequentially(List<Either<String, MessageNode>> segments) {
        return Flux.fromIterable(segments)
                .concatMap(this::sendSegment);
    }
    private Mono<Void> sendSegment(Either<String, MessageNode> segment) {
        return segment.isLeft() ?
                  sendTextSegment(segment.getLeft())
                : sendFileResourceSegment(segment.getRight());
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
                    var messageWrapper = new MessageWrapper(msg);
                    addMessageBubble(messageWrapper);
                    scrollPane.setVvalue(1.0);
                }))
                .then();
    }

    /**
     * 文件段落的处理
     * @param messageNode
     * @return Mono
     */
    private Mono<Void> sendFileResourceSegment(MessageNode messageNode) {
        var uuid = UUID.randomUUID();
        ByteArrayResource resource = new ByteArrayResource(messageNode.getBytes()) {
            @Override public String getFilename() { return messageNode.getDescription(); }
        };

        return fileEndpoint.upload(resource, uuid)
                .flatMap(resp -> {
                    var msg = Chat.ChatMessage.newBuilder()
                            .setConversationId(getConversationId())
                            .setFromAccountInfo(UserInfoContext.getAccountInfo())
                            .setType(messageNode.getType())
                            .setContent(resp.getId().toString())
                            .build();

                    var messageWrapper = new MessageWrapper(msg);

                    var pkg = BaseMessage.BaseMessagePkg.newBuilder().setMessage(msg).build();

                    return ClientToolkit.reactiveClientAction()
                            .sendMessage(pkg)
                            .doOnTerminate(() -> Platform.runLater(() -> {
                                addMessageBubble(messageWrapper);
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
//    private  void addMessageBubble(Object message) {
//        var chatBubblePane = applicationContext.getBean(ChatBubblePane.class, message);
//
//        Platform.runLater(() -> messageDisplayArea.getChildren()
//                .add(chatBubblePane));
//    }

    /**
     * Adds a new message bubble to the display area on the JavaFX Application Thread.
     * @param sender message sender
     * @param message  message content
     */
//    private void addMessageBubble( String sender , Object message) {
//        var chatBubblePane = applicationContext.getBean(ChatBubblePane.class, sender ,message);
//        Platform.runLater(() -> messageDisplayArea.getChildren().add(chatBubblePane));
//    }
    private void addMessageBubble( MessageWrapper messageWrapper) {
//        var chatBubblePane = applicationContext
//                .getBean(ChatBubblePane.class, messageWrapper,
//                        messageNodeService.createMessageNode(messageWrapper));
        var chatBubblePane = new ChatBubblePane(messageWrapper, messageNodeService.createMessageNode(messageWrapper));

        Platform.runLater(() -> messageDisplayArea.getChildren().add(chatBubblePane));
    }



    @PostConstruct
    public void initialize() {
        setupMessageListener();
        setupMessageDisplayArea();
        setupMessageSendArea();
        setupScrollPane();
        setupSendPane();
        setupLayout();
        setupClipboardAndKeyHandlers();
    }

    /** 监听聊天消息事件 */
    private void setupMessageListener() {
        receiveChatMessageEvent()
                .subscribeOn(Schedulers.boundedElastic())
                .subscribe(
                        null,
                        err -> log.error("消息接收流程异常", err)
                );
    }

    /** 初始化消息展示区域（VBox） */
    private void setupMessageDisplayArea() {
        messageDisplayArea = new VBox(10);
        messageDisplayArea.setPadding(new Insets(10));
        VBox.setVgrow(messageDisplayArea, Priority.ALWAYS);

        // 消息区域变动时滚动到底部
        messageDisplayArea.heightProperty().addListener((obs, oldVal, newVal) -> {
            scrollPane.setVvalue(1.0);
        });
    }

    /** 初始化发送消息输入框 */
    private void setupMessageSendArea() {
        messageSendArea = new RichTextMessageArea();
        messageSendArea.setWrapText(true);

        messageSendArea.setOnKeyPressed(event -> {
            switch (event.getCode()) {
                case ENTER -> {
                    if (event.isShiftDown()) {
                        messageSendArea.insertText(messageSendArea.getCaretPosition(), "\n");
                    } else {
                        event.consume(); // 阻止默认行为
                        sendMessage().subscribe();
                    }
                }
            }
        });
    }

    /** 初始化 ScrollPane 包裹 messageDisplayArea */
    private void setupScrollPane() {
        scrollPane = new MFXScrollPane(messageDisplayArea);
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefHeight(300);
        ScrollUtils.addSmoothScrolling(scrollPane);
    }

    /** 初始化发送区域面板 */
    private void setupSendPane() {
        sendMessagePane = new SendMessagePane(sendMessage());
        sendMessagePane.setPrefHeight(50);
        sendMessagePane.prefHeightProperty().bind(
                Bindings.multiply(this.heightProperty(), 0.1)
        );
    }

    /** 设置整体布局 */
    private void setupLayout() {
        this.setTop(scrollPane);
        this.setCenter(messageSendArea);
        this.setBottom(sendMessagePane);
    }

    /** 设置剪贴板粘贴监听和处理 */
    private void setupClipboardAndKeyHandlers() {
        messageSendArea.setOnKeyReleased(event -> {
            if (event.isControlDown() && event.getCode() == KeyCode.V) {
                var imageFromClipboard = ClipboardUtils.getImageFromClipboard();
                if (imageFromClipboard != null) {
                    messageSendArea.insertNode(new StreamImage(imageFromClipboard));
                    event.consume();
                }

                var filesFromClipboard = ClipboardUtils.getFilesFromClipboard();
                if (!filesFromClipboard.isEmpty()) {
                    filesFromClipboard.stream()
                            .filter(File::isFile)
                            .forEach(file -> {
                                var localFileInfo = new LocalFileInfo(file.toPath());
                                var fileNode = new FileNode(localFileInfo);
                                messageSendArea.insertNode(fileNode);
                            });
                }
            }
        });
    }



}