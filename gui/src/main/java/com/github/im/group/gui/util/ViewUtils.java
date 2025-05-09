//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.github.im.group.gui.util;

import com.github.im.group.gui.views.FView;
import com.gluonhq.attach.lifecycle.LifecycleService;
import com.gluonhq.attach.util.Platform;
//import com.gluonhq.charm.glisten.afterburner.AppView;
import com.gluonhq.charm.glisten.control.NavigationDrawer;
import com.gluonhq.charm.glisten.visual.MaterialDesignIcon;
import java.util.Collection;
import java.util.Iterator;
import java.util.Objects;
import java.util.function.Consumer;

public class ViewUtils {
    public ViewUtils() {
    }

    public static void buildDrawer(NavigationDrawer drawer, NavigationDrawer.Header header, Collection<FView> views) {
        buildDrawer(drawer, header, views, (view) -> {
        });
    }

    public static void buildDrawer(NavigationDrawer drawer, NavigationDrawer.Header header, Collection<FView> views, Consumer<FView> viewAdded) {
        Objects.requireNonNull(drawer, "NavigationDrawer cannot be null");
        Objects.requireNonNull(views, "Collection of AppView cannot be null");
        Objects.requireNonNull(viewAdded, "viewAdded consumer cannot be null");
        drawer.setHeader(header);
        Iterator var4 = views.iterator();

        while(var4.hasNext()) {
            FView view = (FView)var4.next();
            if (view.isShownInDrawer()) {
                drawer.getItems().add(view.getMenuItem());
                viewAdded.accept(view);
            }
        }

        if (Platform.isDesktop()) {
            NavigationDrawer.Item quitItem = new NavigationDrawer.Item("Quit", MaterialDesignIcon.EXIT_TO_APP.graphic());
            quitItem.selectedProperty().addListener((obs, ov, nv) -> {
                if (nv) {
                    LifecycleService.create().ifPresent(LifecycleService::shutdown);
                }

            });
            drawer.getItems().add(quitItem);
        }

    }
}
