//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.github.im.group.gui.util;

import com.github.im.group.gui.views.AppViewManager;
import com.github.im.group.gui.views.FView;
import com.github.im.group.gui.views.ViewLifeCycle;
import com.gluonhq.attach.lifecycle.LifecycleService;
import com.gluonhq.attach.util.Platform;
//import com.gluonhq.charm.glisten.afterburner.AppView;
import com.gluonhq.charm.glisten.application.AppManager;
import com.gluonhq.charm.glisten.control.Avatar;
import com.gluonhq.charm.glisten.control.NavigationDrawer;
import com.gluonhq.charm.glisten.visual.MaterialDesignIcon;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.image.Image;

import java.util.*;
import java.util.function.Consumer;

/**
 * 用于 对 View  进行一些 菜单栏的 画布操作
 */
public class ViewUtils {

    private static final ResourceBundle bundle = ResourceBundle.getBundle("i18n.drawer");

    private ViewUtils() {
    }

    public static void buildDrawer(NavigationDrawer drawer, NavigationDrawer.Header header, Collection<ViewLifeCycle> views) {
        buildDrawer(drawer, header, views, (view) -> {
        });
    }

    /**
     * 构建
     * @param view
     */
    public static void buildDrawer( ViewLifeCycle view) {

        buildDrawer(Collections.singleton(view));
    }
    public static void buildDrawer( Collection<ViewLifeCycle> views) {
        NavigationDrawer drawer = AppManager.getInstance().getDrawer();
        NavigationDrawer.Header header = new NavigationDrawer.Header(bundle.getString("drawer.header.title"),
                bundle.getString("drawer.header.description"),
                new Avatar(21,
                        new Image(Objects.requireNonNull(AppViewManager.class
                                .getResourceAsStream("/images/icon.png")))));
        header.setStyle("-fx-background-color: white;");

        buildDrawer(drawer, header, views);
    }

    public static void buildDrawer(NavigationDrawer drawer, NavigationDrawer.Header header, Collection<ViewLifeCycle> views, Consumer<ViewLifeCycle> viewAdded) {
        Objects.requireNonNull(drawer, "NavigationDrawer cannot be null");
        Objects.requireNonNull(views, "Collection of AppView cannot be null");
        Objects.requireNonNull(viewAdded, "viewAdded consumer cannot be null");
        drawer.setHeader(header);

        var items = drawer.getItems();
        for (ViewLifeCycle view : views) {
//            FView view = (FView) viewLifeCycle;
            if (view.isShownInDrawer()) {
                items.add(view.getMenuItem());
                viewAdded.accept(view);
            }
        }

//        if (Platform.isDesktop()) {
//            NavigationDrawer.Item quitItem = new NavigationDrawer.Item("Quit", MaterialDesignIcon.EXIT_TO_APP.graphic());
//            quitItem.selectedProperty().addListener((obs, ov, nv) -> {
//                if (nv) {
//                    LifecycleService.create().ifPresent(LifecycleService::shutdown);
//                }
//
//            });
//            items.add(quitItem);
//        }

    }
}
