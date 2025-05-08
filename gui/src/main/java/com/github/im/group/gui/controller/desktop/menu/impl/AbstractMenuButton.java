package com.github.im.group.gui.controller.desktop.menu.impl;

import com.github.im.group.gui.controller.desktop.menu.MenuButton;
import io.github.palexdev.materialfx.controls.MFXRectangleToggleNode;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.image.ImageView;
import lombok.NonNull;
import lombok.Setter;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

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

@Component
public class AbstractMenuButton extends MFXRectangleToggleNode  implements MenuButton, ApplicationContextAware {

    static final ResourceBundle menuBundle = ResourceBundle.getBundle("i18n.menu.button");


    public static Sinks.Many<Class<? extends AbstractMenuButton>> SWITCH_BUTTON = Sinks.many().multicast().onBackpressureBuffer();


    private ApplicationContext applicationContext;

    @Setter
    private ToggleGroup group;

    @Override
    public void setApplicationContext(@NonNull ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }


    public Flux<Class<? extends AbstractMenuButton>> getEventSink() {
        return SWITCH_BUTTON.asFlux();
    }

    public void sendEvent(Class<? extends AbstractMenuButton> event) {
        // 发布as事件到 Sink
        SWITCH_BUTTON.tryEmitNext(event);
    }

    public ImageView getButtonIcon() {

        ImageView icon = new ImageView();
        icon.setFitWidth(25);
        icon.setFitHeight(25);

        return icon;
    }



    public AbstractMenuButton() {
        initializeButton();
    }

    public Tooltip tooltip() {
        return new Tooltip("Button");
    }


    void initializeButton() {


//         MFXRectangleToggleNode toggleNode = new MFXRectangleToggleNode(text);
        this.setAlignment(Pos.CENTER);

        this.setGraphic(getButtonIcon());
        this.setText(null);
        this.setMaxSize(50, 50);
        this.setPrefSize(50, 50);
        this.setTooltip(tooltip());
        this.setPadding(new Insets(0, 0, 0, 0));
//        this.getStyleClass().add("menu-button"); // 确保有 CSS 样式

    }


    /**
     * 直接从 Spring 容器获取所有按钮，避免循环依赖
     */
    public <T extends AbstractMenuButton> List<Tuple2<Class<? extends AbstractMenuButton>,ToggleButton>> getAllButtonTuplesList() {
        return List.of(
                toggleButtonTuple2(ChatButton.class),
                toggleButtonTuple2(MailButton.class),
                toggleButtonTuple2(ContactsButton.class),
                toggleButtonTuple2(DocumentsButton.class),
                toggleButtonTuple2(ScheduleButton.class),
                toggleButtonTuple2(MeetingsButton.class),
                toggleButtonTuple2(WorkbenchButton.class)
        );
    }


    private <T extends AbstractMenuButton> Tuple2<Class<? extends AbstractMenuButton>,ToggleButton> toggleButtonTuple2(Class<T> buttonClass) {
        return Tuples.of(buttonClass,applicationContext.getBean(buttonClass));

    }

    public List<ToggleButton> getAllButtons() {
        return List.of(
                applicationContext.getBean(ChatButton.class),
                applicationContext.getBean(MailButton.class),
                applicationContext.getBean(ContactsButton.class),
                applicationContext.getBean(DocumentsButton.class),
                applicationContext.getBean(ScheduleButton.class),
                applicationContext.getBean(MeetingsButton.class),
                applicationContext.getBean(WorkbenchButton.class)
        );
    }

}