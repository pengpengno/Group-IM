package com.github.im.group.gui.controller.desktop.menu.impl;

import com.github.im.group.gui.controller.desktop.menu.MenuButton;
import io.github.palexdev.materialfx.controls.MFXButton;
import io.github.palexdev.mfxresources.fonts.MFXFontIcon;
import javafx.geometry.Insets;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;

import java.util.Objects;
import java.util.ResourceBundle;

public class ScheduleButton extends  AbstractMenuButton  {



    public ScheduleButton() {
        initializeButton();
     }

    @Override
    public ImageView getButtonIcon() {
        var buttonIcon = super.getButtonIcon();
        var icon  = new Image(Objects.requireNonNull(getClass().
                getResourceAsStream("/" + menuBundle.getString("schedule.icon"))));
        buttonIcon.setImage(icon);
        return buttonIcon;
    }

    @Override
    public Tooltip tooltip() {
        var tooltip = super.tooltip();
        tooltip.setText(menuBundle.getString("schedule.tooltip"));
        return tooltip;
    }
}
