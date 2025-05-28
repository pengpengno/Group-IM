package com.github.im.group.gui.views;


import com.github.im.group.gui.controller.desktop.chat.ChatMainPresenter;
import com.gluonhq.charm.glisten.application.AppManager;
import com.gluonhq.charm.glisten.control.*;
import com.gluonhq.charm.glisten.mvc.View;
import com.gluonhq.charm.glisten.visual.MaterialDesignIcon;
import javafx.collections.FXCollections;
import javafx.scene.control.*;
import javafx.scene.control.MenuItem;
import javafx.scene.layout.VBox;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.Locale;
import java.util.ResourceBundle;

@Slf4j
@Service
@RequiredArgsConstructor
public class SettingPresenter extends View  implements com.github.im.group.gui.views.MenuItem {

    // 创建下拉框
    ComboBox<Locale> languageSelector = new ComboBox<>(FXCollections.observableArrayList(
            Locale.ENGLISH, Locale.SIMPLIFIED_CHINESE
    ));


    /**
     *
     * TODO 配置项容器
     */
    private VBox menuBox;

    private Label languageLabel;


    @Getter
    private final ToggleGroup toggleGroup = new ToggleGroup();


    private ResourceBundle bundle = ResourceBundle.getBundle("i18n.menu.button");

    private final ChatMainPresenter chatMainPresenter;


    public BottomNavigation bottomNavigation () {
        BottomNavigation bottomNav = new BottomNavigation();

        var type = bottomNav.getType();
        // 创建底部按钮
        BottomNavigationButton message =
                new BottomNavigationButton("聊天", MaterialDesignIcon.MESSAGE.graphic());


        BottomNavigationButton people =
                new BottomNavigationButton("联系人", MaterialDesignIcon.PEOPLE.graphic());


        BottomNavigationButton mine =
                new BottomNavigationButton("我的", MaterialDesignIcon.PERSON.graphic());
//        loginView.setCenter(peopleView);
        people.setSelected(true);

        // 添加按钮到底部导航栏
        bottomNav.getActionItems().addAll(message,people,mine);

        return bottomNav;

    }

    public void initialAppBar() {

        // 底部导航栏
        var isMobile = !com.gluonhq.attach.util.Platform.isDesktop();
        // 移动端底部栏  客户端侧边栏
        if(isMobile){

            this.setBottom(bottomNavigation());

        }else{

            this.setBottom(bottomNavigation());

        }

        this.showingProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue) {

                AppBar appBar = AppManager.getInstance().getAppBar();

                appBar.setNavIcon(MaterialDesignIcon.MENU.button(e -> System.out.println("nav icon")));

                appBar.setTitleText("The AppBar");

                appBar.getActionItems().addAll(
                        MaterialDesignIcon.SEARCH.button(e -> System.out.println("search")),
                        MaterialDesignIcon.FAVORITE.button(e -> System.out.println("fav")));

                appBar.getMenuItems().addAll(new MenuItem("Settings"));

                appBar.setNavIcon(MaterialDesignIcon.MENU.button(e ->
                        AppManager.getInstance().getDrawer().open()));

                appBar.setTitleText("主页");
            }
        });
    }



}
