package com.github.im.group.gui.controller.chat;

import com.github.im.common.connect.connection.client.ClientToolkit;
import com.github.im.common.connect.connection.client.ReactiveClientAction;
import com.github.im.common.connect.model.proto.Account;
import com.github.im.common.connect.model.proto.BaseMessage;
import com.github.im.common.connect.model.proto.Chat;
import com.github.im.dto.user.UserInfo;
import com.github.im.group.gui.connect.handler.EventBus;
import com.github.im.group.gui.context.UserInfoContext;
import com.jfoenix.controls.JFXTextArea;
import io.github.palexdev.materialfx.controls.MFXButton;
import io.github.palexdev.materialfx.controls.MFXScrollPane;
import io.github.palexdev.materialfx.enums.ButtonType;
import io.github.palexdev.materialfx.utils.ScrollUtils;
import jakarta.annotation.PostConstruct;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * Description:
 * <p>
 *     chat message pane , include  message send area , messageDisplay Area;
 * </p>
 *
 * @author pengpeng
 * @version 1.0
 * @since 2025/1/9
 */
@Component
@Scope("prototype")
@Slf4j
public class ChatMessagePane extends BorderPane implements Initializable {


    private JFXTextArea messageSendArea; // message send area

    @Getter
    @Setter
    private UserInfo toAccountInfo;

    private VBox messageDisplayArea; // 设置每条消息之间的间距

    private MFXScrollPane scrollPane; // 设置每条消息之间的间距

    private SendMessagePane sendMessagePane;


    @Autowired
    private EventBus bus;

    @Autowired
    private ReactiveClientAction clientAction; ;



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
     * receive chat message Event
     * @return chat message event mono
     */
    public Mono<Void>  receiveChatMessageEvent() {
        return bus.asFlux().ofType(Chat.ChatMessage.class )
                .filter(chatmessage -> chatmessage.getToAccountInfo().getAccount()
                        .equals(getToAccountInfo().getUsername()))
                .doOnNext(chatmessage -> {
                    var fromAccountInfo = chatmessage.getFromAccountInfo();
                    var account = fromAccountInfo.getAccount();
                    addMessageBubble(account, chatmessage.getContent()); // 添加消息气泡
                })
                .then()
                ;
    }

    /**
     * 发送消息
     *
     * @return
     */
    private Mono<Void> sendMessage() {
        // 获取输入消息
        return Mono.defer(()-> Mono.fromCallable(()->messageSendArea.getText()))
                .filter(message -> !message.isBlank()) // 过滤空消息
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Message cannot be blank")))
                .flatMap(message -> {
                    // 获取目标用户信息
                    return Mono.justOrEmpty(getToAccountInfo())
                            .switchIfEmpty(Mono.error(new IllegalArgumentException("Target user is not selected")))
                            .map(userInfo -> {
                                var accountInfo = Account.AccountInfo.newBuilder()
                                        .setUserId(userInfo.getUserId())
                                        .setAccount(userInfo.getUsername())
                                        .build();
                                var chatMessage = Chat.ChatMessage.newBuilder()
                                        .setToAccountInfo(accountInfo)
                                        .setFromAccountInfo(UserInfoContext.getAccountInfo())
                                        .setContent(message)
                                        .build();
                                return BaseMessage.BaseMessagePkg.newBuilder()
                                        .setMessage(chatMessage)
                                        .build();
                            })
                            .flatMap(baseChatMessage -> {
                                // 发送消息
                                return ClientToolkit.reactiveClientAction()
                                        .sendMessage(baseChatMessage)
                                        .subscribeOn(Schedulers.boundedElastic()) // 网络请求放入后台线程池

                                        .doOnSuccess(response -> {
                                            // 更新 UI
                                            Platform.runLater(() -> {
                                                addMessageBubble(message); // 添加消息气泡
                                                messageSendArea.clear();   // 清空输入框
                                                scrollPane.setVvalue(1.0); // 滚动到底部
                                            });
                                        });
                            });
                })
                .then()
                .onErrorResume(e -> {
                    log.error("Failed to send message: {}", e.getMessage());
                    return Mono.empty(); // 忽略错误，或者显示提示
                })
                .checkpoint()
                ;
    }


    /**
     * Adds a new message bubble to the display area on the JavaFX Application Thread.
     *
     * @param message            The message content
     */
    private   void addMessageBubble( String message) {
        Platform.runLater(() -> messageDisplayArea.getChildren().add(new ChatBubblePane(message)));
    }

    /**
     * Adds a new message bubble to the display area on the JavaFX Application Thread.
     * @param sender message sender
     * @param message  message content
     */
    private  void addMessageBubble( String sender , String message) {
        Platform.runLater(() -> messageDisplayArea.getChildren().add(new ChatBubblePane(sender,message)));
    }





    @Override
    public void initialize(URL location, ResourceBundle resources) {

    }

    @PostConstruct
    public void initialize() {
        // 监听聊天消息事件
        receiveChatMessageEvent().subscribe();


        // Initialize message display area
        messageDisplayArea = new VBox(10);
        messageDisplayArea.setPadding(new Insets(10)); // 设置内边距


        // Initialize send message area
//        messageSendArea = new MFXTextField();
        messageSendArea = new JFXTextArea();

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

        this.setTop(scrollPane);
        this.setCenter(messageSendArea);
        this.setBottom(sendMessagePane);

    }


}