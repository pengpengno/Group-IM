package com.github.im.group.gui;

import io.github.palexdev.materialfx.controls.MFXButton;
import io.github.palexdev.materialfx.controls.MFXTextField;
import io.github.palexdev.materialfx.theming.JavaFXThemes;
import io.github.palexdev.materialfx.theming.MaterialFXStylesheets;
import io.github.palexdev.materialfx.theming.UserAgentBuilder;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class MaterialFxApp extends Application {
    @Override
    public void start(Stage primaryStage) {
        // Configure the UserAgentBuilder
        UserAgentBuilder.builder()
                .themes(JavaFXThemes.MODENA)
                .themes(MaterialFXStylesheets.forAssemble(true))
                .setDeploy(true)
                .setResolveAssets(true)
                .build()
                .setGlobal();

        // Create a VBox layout
        VBox root = new VBox(10);
        root.setPadding(new javafx.geometry.Insets(20));

        // Create MaterialFX components
        MFXTextField textField = new MFXTextField();
        textField.setPromptText("Enter your name");

        MFXButton button = new MFXButton("Click Me");
        button.setOnAction(event -> System.out.println("Button Clicked!"));

        // Add components to the layout
        root.getChildren().addAll(textField, button);

        // Create and set the scene
        Scene scene = new Scene(root, 400, 300);
        primaryStage.setTitle("MaterialFX Example");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}