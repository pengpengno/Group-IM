package com.github.im.group.gui.controller.desktop.meeting;

import io.github.palexdev.materialfx.controls.MFXButton;
import javafx.scene.layout.GridPane;
import javafx.scene.web.WebView;

import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * Description:
 * <p>
 * </p>
 *
 * @author pengpeng
 * @version 1.0
 * @since 2025/5/6
 */
public class MeetingMainView  extends GridPane {

    private MFXButton startButton;

    private MFXButton endButton;


    private WebView webView;


    public MeetingMainView() {
        startButton = new MFXButton("开始");
        startButton.setOnAction(event-> {
            webView = new WebView();
            webView.getEngine().load("http://localhost:8080/static/chat.html");
            var desktop = Desktop.getDesktop();

            try {
                desktop.browse(new URI("http://localhost:8080/static/chat.html"));
            } catch (IOException e) {
                throw new RuntimeException(e);
            } catch (URISyntaxException e) {
                throw new RuntimeException(e);
            }

            this.getChildren().add(webView);

        });


        this.setWidth(500);
        this.setHeight(500);
        this.getChildren().addAll(startButton);

    }



}