package com.github.im.group.gui.controller.desktop.menu;

import io.github.palexdev.materialfx.controls.MFXButton;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.util.Objects;

/**
 * Description:
 * <p>
 * </p>
 *
 * @author pengpeng
 * @version 1.0
 * @since 2025/2/25
 */
public interface MenuButton {



    default Button getButton(String text){

        var mfxButton = new MFXButton();
        mfxButton.setText(null);
        mfxButton.setPrefSize(50, 50);
        mfxButton.setPadding(new Insets(0,0,0,0));
        return mfxButton;
    }

    public default int order(){
        return Integer.MAX_VALUE;
    }






}