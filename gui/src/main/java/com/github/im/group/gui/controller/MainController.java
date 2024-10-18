package com.github.im.group.gui.controller;

import com.github.im.group.gui.util.FxView;
import com.github.im.group.gui.util.FxmlLoader;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
@Slf4j
@FxView(path = "main_layout")
public class MainController {


    @FXML
    private VBox sideBar;

    @FXML
    private Button chatButton;

    @FXML
    private Button contactsButton;

    @FXML
    private Button workButton;

    @FXML
    private ListView<String> chatListView;

    @FXML
    private void initialize() {
        // 初始化时添加示例数据
        chatListView.getItems().addAll("Chat 1", "Chat 2", "Chat 3");

        // 按钮点击事件示例
        chatButton.setOnAction(event -> handleChatClick());
        contactsButton.setOnAction(event -> handleContactsClick());
        workButton.setOnAction(event -> handleWorkClick());

    }

    private void handleChatClick() {
        System.out.println("Chat Button Clicked");
    }

    private void handleContactsClick() {
        System.out.println("Contacts Button Clicked");
    }

    private void handleWorkClick() {
        System.out.println("Work Button Clicked");
    }


    public void show() {

    }
}