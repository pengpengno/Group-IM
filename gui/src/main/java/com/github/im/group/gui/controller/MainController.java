package com.github.im.group.gui.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;

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
        try {
            // 加载主界面的 FXML 文件
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/MainView.fxml"));
            Parent root = loader.load();

            // 创建新窗口并设置场景
            Stage stage = new Stage();
            stage.setTitle("主界面");
            stage.setScene(new Scene(root));

            // 显示主界面
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            // 处理加载失败的情况
        }
    }
}