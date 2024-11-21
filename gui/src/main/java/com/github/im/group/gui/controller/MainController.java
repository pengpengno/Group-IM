package com.github.im.group.gui.controller;

import com.github.im.dto.user.FriendshipDTO;
import com.github.im.group.gui.api.FriendShipEndpoint;
import com.github.im.group.gui.context.UserInfoContext;
import com.github.im.group.gui.util.FxView;
import com.github.im.group.gui.util.FxmlLoader;
import javafx.application.Platform;
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
import java.util.List;

@Service
@Slf4j
@FxView(path = "main_layout")
public class MainController {
    @Autowired
    private FriendShipEndpoint friendShipEndpoint;

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
        // Fetch and render friend list upon initialization
        loadFriendList();

        // Setup button click events
        chatButton.setOnAction(event -> handleChatClick());
        contactsButton.setOnAction(event -> handleContactsClick());
        workButton.setOnAction(event -> handleWorkClick());
    }

    private void loadFriendList() {
        // Assume we have a method to get the logged-in user’s ID
        Long userId = getCurrentUserId(); // Replace this with actual retrieval method

        // Call the API to get the list of friends
        friendShipEndpoint.getFriends(userId)
                .doOnError(error -> log.error("Failed to load friends", error))
                .doOnSuccess(this::updateFriendList)
                .subscribe();
    }

    private void updateFriendList(List<FriendshipDTO> friendships) {
        Platform.runLater(() -> {
            // Clear existing items
            chatListView.getItems().clear();

            // Populate the ListView with friend names
            friendships.forEach(friendship -> {
                String friendName = friendship.getFriendUserInfo().getUsername();
                chatListView.getItems().add(friendName);
            });
        });
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
        // Optional: Add logic to display this view if needed
    }

    private Long getCurrentUserId() {
        // Placeholder for retrieving the current user’s ID
        return UserInfoContext.getCurrentUser().getUserId(); // Replace this with the actual user ID retrieval logic
    }
}