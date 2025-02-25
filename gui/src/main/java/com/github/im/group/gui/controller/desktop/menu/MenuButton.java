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
//        ImageView icon = new ImageView(new Image(Objects.requireNonNull(getClass().getResourceAsStream("/" + "images/main/toolbox/message.png"))));
//        icon.setFitWidth(24);
//        icon.setFitHeight(24);

//        Button button = new Button();
        mfxButton.setText(null);
//        button.setButtonType(ButtonType.RAISED);
//        mfxButton.setGraphic(icon);
        mfxButton.setPrefSize(50, 50);
//        mfxButton.setTooltip(new Tooltip(bundle.getString("chat.text")));
        mfxButton.setPadding(new Insets(0,0,0,0));
        return mfxButton;
    }

    public default int order(){
        return Integer.MAX_VALUE;
    }






}