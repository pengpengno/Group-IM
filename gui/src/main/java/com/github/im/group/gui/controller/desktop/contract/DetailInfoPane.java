package com.github.im.group.gui.controller.desktop.contract;

import com.github.im.dto.user.UserInfo;
import com.github.im.group.gui.api.ConversationEndpoint;
import com.github.im.group.gui.connect.handler.EventBus;
import com.github.im.group.gui.context.UserInfoContext;
import com.github.im.group.gui.controller.desktop.menu.impl.AbstractMenuButton;
import com.github.im.group.gui.controller.desktop.menu.impl.ChatButton;
import com.github.im.group.gui.util.AvatarGenerator;
import com.github.im.group.gui.util.I18nUtil;
import io.github.palexdev.materialfx.controls.MFXButton;
import jakarta.annotation.PostConstruct;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.util.ResourceBundle;

@Component
@Slf4j
@RequiredArgsConstructor
public class DetailInfoPane extends GridPane {




//    private final ChatMainPane chatMainPane;

    private final ConversationEndpoint conversationEndpoint;

    private final EventBus bus;

    private ImageView avatarGenerator;
    private Label phone = new Label();
    private Label mail = new Label();
    private Label name = new Label();

    private MFXButton sendMessage = new MFXButton();
    private MFXButton sendMail = new MFXButton();
    private MFXButton call = new MFXButton();


    private UserInfo  userInfo;

//    private ResourceBundle resourceBundle = I18nUtil.getResourceBundle("i18n.contract.contract");
    static final ResourceBundle resourceBundle = ResourceBundle.getBundle("i18n.contract.contract");




    @PostConstruct
    public void initPane() {
        // 设置 GridPane 对齐方式为 TOP_CENTER
        this.setAlignment(Pos.TOP_CENTER);
        this.setPadding(new Insets(10));  // 设置内边距
        this.setHgap(15);  // 设置列间距
        this.setVgap(10);  // 设置行间距

        // 预定义列宽
        ColumnConstraints col1 = new ColumnConstraints();
        col1.setMinWidth(100);
        col1.setPrefWidth(150);
        col1.setMaxWidth(200);

        ColumnConstraints col2 = new ColumnConstraints();
        col2.setHgrow(javafx.scene.layout.Priority.ALWAYS);  // 让这一列自动扩展

        this.getColumnConstraints().addAll(col1, col2);

        // 预定义行高
        RowConstraints row1 = new RowConstraints();
        row1.setPrefHeight(50);
        this.getRowConstraints().add(row1);


        //  chatButton 激活
        sendMessage.setOnAction(event-> {

            log.debug("click and   switch  button ");

            AbstractMenuButton.sendEvent(ChatButton.class);

            conversationEndpoint.createOrGetPrivateChat(UserInfoContext.getAccountInfo().getUserId() , this.userInfo.getUserId())
                    .doOnError(error -> log.error("Failed to create or get private chat", error))
                    .doOnSuccess(conversationRes -> {
                        bus.publish(conversationRes);
//                        chatMainPane.updateConversations(conversationRes);

                    })
                    .subscribe();


        });
        // 初始化按钮
        sendMessage.setText(resourceBundle.getString("contract.DetailInfoPane.sendButton.text"));
        sendMail.setText(resourceBundle.getString("contract.DetailInfoPane.sendMail.text"));
        call.setText("拨打电话");
    }

    public void display(UserInfo userInfo) {


        this.getChildren().clear(); // 清空旧的 UI 组件

        this.userInfo = userInfo;

        name.setText(userInfo.getUsername());
        avatarGenerator = new ImageView(AvatarGenerator.generateSquareAvatarWithRoundedCorners(userInfo.getUsername(), 40));

        phone.setText("tel:" + userInfo.getEmail());
        mail.setText("email" + userInfo.getEmail());
        mail.setAlignment(Pos.CENTER);
        phone.setAlignment(Pos.CENTER);

        // 头像 + 名字，居中对齐
        HBox avatarBox = new HBox(10, avatarGenerator, name);
        avatarBox.setAlignment(Pos.CENTER);


        VBox infoBox = new VBox(10, phone, mail);
        infoBox.setAlignment(Pos.CENTER);

        // 按钮组，居中对齐
        HBox buttonBox = new HBox(10, sendMessage, sendMail, call);

        buttonBox.setAlignment(Pos.CENTER);

        // 添加组件到 GridPane，内容居中
        this.add(avatarBox, 0, 0, 2, 1);  // 跨两列
        this.add(infoBox, 0, 1, 2, 1);  // 跨两列
//        this.add(phone, 0, 1);
//        this.add(mail, 0, 2);
        this.add(buttonBox, 0, 2, 2, 1);  // 按钮跨两列



        // 调整行列策略，避免 UI 变形
        this.getColumnConstraints().get(0).setHalignment(javafx.geometry.HPos.CENTER);  // Set column alignment to center

    }
}
