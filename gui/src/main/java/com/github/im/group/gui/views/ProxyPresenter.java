package com.github.im.group.gui.views;


import com.github.im.group.gui.config.ServerConnectProperties;
import com.gluonhq.charm.glisten.application.AppManager;
import com.gluonhq.charm.glisten.control.AppBar;
import com.gluonhq.charm.glisten.mvc.View;
import com.gluonhq.charm.glisten.visual.MaterialDesignIcon;
import io.github.palexdev.materialfx.controls.MFXButton;
import jakarta.annotation.PostConstruct;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class ProxyPresenter extends View  {


    private final Label label_Host = new Label("host");
    private  final TextField host = new TextField();
    private  final Button confirmButton = new MFXButton("Confirm");

    private final ServerConnectProperties serverConnectProperties;

    /**
     * Gluon 会自动跳动这个方案
     */
    @PostConstruct
    public void initialize() {

        var hBox = new HBox();
        hBox.getChildren().addAll(label_Host , host);
        hBox.setAlignment(Pos.CENTER_LEFT);

        confirmButton.setOnAction(event -> {
            Platform.runLater(() -> {
                serverConnectProperties.getRest().setHost(host.getText());
                serverConnectProperties.getConnect().setHost(host.getText());
                log.info("host {}",serverConnectProperties.getRest().getHost());
                log.info("host {}",serverConnectProperties.getConnect().getHost());
//                AppViewManager.switchView(LoginPresenter.class);
                AppViewManager.switchView(AppManager.HOME_VIEW);
//                DisplayManager.display(LoginPresenter.class);
            });
        });

        setCenter(hBox);
        setBottom(confirmButton);
    }

    @Override
    protected void updateAppBar(AppBar appBar) {
        super.updateAppBar(appBar);

        appBar.setNavIcon(MaterialDesignIcon.MENU.button(e -> System.out.println("nav icon")));

        appBar.setTitleText("The AppBar");

        appBar.getActionItems().addAll(
                MaterialDesignIcon.SEARCH.button(e -> System.out.println("search")),
                MaterialDesignIcon.FAVORITE.button(e -> System.out.println("fav")));

        appBar.getMenuItems().addAll(new javafx.scene.control.MenuItem("Settings"));

        appBar.setNavIcon(MaterialDesignIcon.MENU.button(e ->
                AppManager.getInstance().getDrawer().open()));

        appBar.setTitleText("聊天");
//        initComponent();
    }

}
