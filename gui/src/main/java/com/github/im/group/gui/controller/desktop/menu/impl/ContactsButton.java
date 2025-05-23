package com.github.im.group.gui.controller.desktop.menu.impl;

import com.github.im.group.gui.controller.DisplayManager;
import com.github.im.group.gui.controller.MainHomeView;
import com.github.im.group.gui.views.MainPresenter;
import com.github.im.group.gui.controller.desktop.contract.ContractMainPane;
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
public class ContactsButton extends AbstractMenuButton{

    @Autowired
    private ContractMainPane contractMainPane;


    public ContactsButton() {
        super();

        this.setOnMouseClicked(event -> {

//            if (this.isSelected()){
//                return;
//            }


            log.info("click  Contacts ");

            var controller = DisplayManager.getController(MainPresenter.class);

//            contractMainPane.initComponent();
            contractMainPane.loadContacts();

            var d  = (MainPresenter)controller;

            d.switchRootPane(contractMainPane);

        });
    }

    @Override
    public ImageView getButtonIcon() {
        var buttonIcon = super.getButtonIcon();
        var icon  = new Image(Objects.requireNonNull(getClass().
                getResourceAsStream("/" + menuBundle.getString("contacts.icon"))));
        buttonIcon.setImage(icon);
        return buttonIcon;
    }

    @Override
    public Tooltip tooltip() {
        var tooltip = super.tooltip();
        tooltip.setText(menuBundle.getString("contacts.tooltip"));
        return tooltip;
    }



}
