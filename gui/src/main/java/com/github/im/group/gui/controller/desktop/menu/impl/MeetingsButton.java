package com.github.im.group.gui.controller.desktop.menu.impl;

import com.github.im.group.gui.context.UserInfoContext;
import com.github.im.group.gui.controller.DisplayManager;
import com.github.im.group.gui.controller.MainHomeView;
import com.github.im.group.gui.controller.desktop.DesktopMainView;
import com.github.im.group.gui.controller.desktop.meeting.MeetingMainView;
import javafx.event.ActionEvent;
import javafx.event.EventType;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.awt.*;
import java.net.URI;
import java.util.Objects;


@Component
@Slf4j
public class MeetingsButton extends AbstractMenuButton {

    public MeetingsButton() {
        super();


        this.setOnMouseClicked(event -> {
            log.debug("click button {} ",this.getClass().getName());
            ;
            trigger();
        });

        this.addEventHandler(ActionEvent.ACTION , event -> {
            trigger();
        });
    }

    @Override
    public ImageView getButtonIcon() {
        var buttonIcon = super.getButtonIcon();
        var icon  = new Image(Objects.requireNonNull(getClass().
                getResourceAsStream("/" + menuBundle.getString("meeting.icon"))));
        buttonIcon.setImage(icon);
        return buttonIcon;
    }

    @Override
    public Tooltip tooltip() {
        var tooltip = super.tooltip();
        tooltip.setText(menuBundle.getString("meeting.tooltip"));
        return tooltip;
    }

    private void trigger() {


        var controller = DisplayManager.getController(MainHomeView.class);
        var desktopMainView  = (DesktopMainView)controller;
        desktopMainView.switchRootPane(new MeetingMainView());
    }

}
