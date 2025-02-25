package com.github.im.group.gui.controller.desktop.menu.impl;

import com.github.im.group.gui.controller.desktop.menu.MenuButton;
import io.github.palexdev.materialfx.controls.MFXButton;
import javafx.geometry.Insets;
import javafx.scene.control.Tooltip;
import javafx.scene.image.ImageView;

import java.util.List;
import java.util.ResourceBundle;

/**
 * Description:
 * <p>
 * </p>
 *
 * @author pengpeng
 * @version 1.0
 * @since 2025/2/25
 */
public class AbstractMenuButton extends MFXButton  implements MenuButton {

    static final ResourceBundle menuBundle = ResourceBundle.getBundle("i18n.menu.button");


    public ImageView getButtonIcon(){

        ImageView icon = new ImageView();
        icon.setFitWidth(24);
        icon.setFitHeight(24);

        return icon;
    }


    public AbstractMenuButton() {
        initializeButton();
    }

    public Tooltip tooltip() {
        Tooltip tooltip = new Tooltip("Button");
        return tooltip;
    }


     void initializeButton() {
        this.setGraphic(getButtonIcon());
        this.setText(null);
        this.setPrefSize(50, 50);
        this.setTooltip(getTooltip());
        this.setPadding(new Insets(0, 0, 0, 0));
        this.getStyleClass().add("menu-button"); // 确保有 CSS 样式

     }




    public static List<MFXButton>  getAllButtons() {

        return List.of(
                new ChatButton(),
                new MailButton(),
                new ContactsButton(),
                new DocumentsButton(),
                new ScheduleButton(),
                new MeetingsButton(),
                new WorkbenchButton()
        );
    }

}