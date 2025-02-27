package com.github.im.group.gui.controller.desktop.menu.impl;

import com.github.im.group.gui.controller.Display;
import com.github.im.group.gui.controller.MainHomeView;
import com.github.im.group.gui.controller.desktop.DesktopMainView;
import com.github.im.group.gui.controller.desktop.chat.ChatMainPane;
import javafx.event.EventType;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
@Slf4j
public class ChatButton extends AbstractMenuButton {

    @Autowired
    private ChatMainPane chatMainPane;


    public ChatButton() {
        super();

        this.setOnMouseClicked(event -> {

//            if (this.isSelected()){
//                return;
//            }

            log.info("click  Contacts ");
            EventType<? extends MouseEvent> eventType = event.getEventType();

            var controller = Display.getController(MainHomeView.class);

//            contractMainPane.initialize();

            var d  = (DesktopMainView)controller;


            chatMainPane.loadFriendList();
            d.switchRootPane(chatMainPane);


        });
    }

    @Override
    public ImageView getButtonIcon() {
        var buttonIcon = super.getButtonIcon();
        var icon  = new Image(Objects.requireNonNull(getClass().
                getResourceAsStream("/" + menuBundle.getString("chat.icon"))));
        buttonIcon.setImage(icon);
        return buttonIcon;
    }

    @Override
    public Tooltip tooltip() {
        var tooltip = super.tooltip();
        tooltip.setText(menuBundle.getString("chat.tooltip"));
        return tooltip;
    }






}
