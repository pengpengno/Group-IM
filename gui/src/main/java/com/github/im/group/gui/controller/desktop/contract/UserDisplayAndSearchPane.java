package com.github.im.group.gui.controller.desktop.contract;

import com.github.im.dto.user.UserInfo;
import io.github.palexdev.materialfx.controls.MFXButton;
import io.github.palexdev.materialfx.controls.MFXScrollPane;
import io.github.palexdev.materialfx.utils.ScrollUtils;
import javafx.geometry.Pos;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import lombok.Setter;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Description:
 * <p>
 * </p>
 *
 * @author pengpeng
 * @version 1.0
 * @since 2025/5/7
 */
public class UserDisplayAndSearchPane extends GridPane {


    private TextField searchTextField;

    private MFXButton seachButton;

    @Setter
    private Function<String, List<UserInfo>> serchFunction;

    @Setter
    private Consumer<UserInfo>  addButtonClickFunction;

//    private List<UserInfo> userInfos;

//    public UserDisplayAndSearchPane() {
//
//    }
    /**
     * 用户信息展示面板
//     * @param userInfos
//     * @param addButtonFunction
     */
    public UserDisplayAndSearchPane() {
        layOut();
        var seachBox = new HBox();
        var textField = new TextField();
        textField.setPromptText("搜索用户");
        seachButton = new MFXButton("搜索");
        seachBox.getChildren().addAll(textField,seachButton);

        HBox.setHgrow(textField, Priority.ALWAYS);


        var vBox = new VBox();
        var mfxScrollPane = new MFXScrollPane(vBox);
        vBox.setFillWidth(true);
        mfxScrollPane.setFitToWidth(true);
        mfxScrollPane.setPrefHeight(300);

        ScrollUtils.addSmoothScrolling(mfxScrollPane);

        seachButton.setOnAction(searchEvent -> {
            var userInfos = serchFunction.apply(textField.getText());

            userInfos.forEach(userInfo -> {

                var accountCard = new AccountCard(userInfo);

                var hBox = new HBox();

                var mfxButton = new MFXButton("添加");
                mfxButton.setOnAction(event -> {
                    // 添加按钮的点击事件
                    addButtonClickFunction.accept(userInfo);
                });

                hBox.getChildren().addAll(accountCard,mfxButton);
                hBox.setAlignment(Pos.CENTER_LEFT);
                HBox.setHgrow(accountCard,javafx.scene.layout.Priority.ALWAYS);
                // 添加到面板上
                vBox.getChildren().add(hBox);


            });


        });

        add(seachBox,0,0);
        add(mfxScrollPane,0,1);

        HBox.setHgrow(seachBox, Priority.ALWAYS);
        HBox.setHgrow(mfxScrollPane, Priority.ALWAYS);
        this.setBackground(new Background(new BackgroundFill(Color.WHITE, null, null)));

    }



    /**
     * 此页面 为 column :1  , row : 2
     *
     * 其中 row0 为搜索框  row1  为展示页面
     */
    public void layOut() {

        var top = new RowConstraints();
        top.setPercentHeight(20);

        var display =  new RowConstraints();
        display.setPercentHeight(80);
        this.getRowConstraints().addAll(top,display);

    }





}