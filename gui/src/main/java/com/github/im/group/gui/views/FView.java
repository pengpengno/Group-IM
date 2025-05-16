//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.github.im.group.gui.views;

import com.github.im.group.gui.controller.PlatformView;
import com.github.im.group.gui.util.FxmlLoader;
import com.gluonhq.charm.glisten.application.AppManager;
import com.gluonhq.charm.glisten.application.ViewStackPolicy;
import com.gluonhq.charm.glisten.control.NavigationDrawer;
import com.gluonhq.charm.glisten.mvc.View;
import com.gluonhq.charm.glisten.visual.MaterialDesignIcon;
import javafx.scene.Node;
import javafx.scene.Parent;
import lombok.Getter;
import lombok.Setter;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Objects;

/**
 * 构建 基于 Fxml 注册的 bean
 */
public final class FView  implements ViewLifeCycle{
    @Getter
    private final String id;
    private final Class<? extends PlatformView> presenterClass;
    private final MaterialDesignIcon menuIcon;

    private final EnumSet<Flag> flags;

    private final ViewStackPolicy defaultViewStackPolicy;
    @Getter
    private final String title;
    private NavigationDrawer.Item menuItem;
    @Getter
    ViewRegistry registry = null;
    // view 的实体对象
    @Getter
    private Object presenter;
    // fxmlloader 解析 fxml文件得到的对象
    private Parent parent;
    @Setter
    private Node menuIconNode;



    public FView(Class<? extends PlatformView> viewClass, MaterialDesignIcon menuIcon, Flag... flags) {
        this.title = viewClass.getSimpleName();

        final var id = viewClass.getSimpleName();
        Objects.requireNonNull(id);
        this.presenterClass = (Class)Objects.requireNonNull(viewClass);
        this.menuIcon = menuIcon;
        this.flags = flags != null && flags.length != 0 ? EnumSet.copyOf(Arrays.asList(flags)) : EnumSet.noneOf(Flag.class);
        this.defaultViewStackPolicy = this.flags.contains(FView.Flag.SKIP_VIEW_STACK) ? ViewStackPolicy.SKIP : ViewStackPolicy.USE;
        boolean isHomeView = this.flags.contains(FView.Flag.HOME_VIEW);
        menuIconNode = menuIcon == null ? null : menuIcon.graphic();
        this.id = isHomeView ? AppManager.HOME_VIEW : ("SPLASH".equals(id) ? "splash" : id );
//        this.id = isHomeView ? AppManager.HOME_VIEW : ("SPLASH".equals(id) ? "splash" : id + "_VIEW");
    }


    @Override
    public ViewStackPolicy getViewStackPolicy() {
        return defaultViewStackPolicy;
    }

    public boolean isShownInDrawer() {
        return this.flags.contains(FView.Flag.SHOW_IN_DRAWER);
    }

    public Class<?> getPresenterClass() {
        return this.presenterClass;
    }


    public void registerView() {
        AppManager.getInstance().addViewFactory(this.id, () -> {
            if(parent == null ){
                var path = ViewRegistry.buildFxmlPath(presenterClass);
                var parentTuple2 = FxmlLoader.loadFxml(path.toString());
                if(parentTuple2 == null){
                    throw new RuntimeException(presenterClass + "FxmlLoader.loadFxml("+path.toString()+") is null");
                }
                parent = parentTuple2.getT2();
                presenter = parentTuple2.getT1().getController();
            }
//            var view = new View(parent);
            // 这里不要 new 出来view 存放 不然会出现两个view
            View view = (View)parent;
            registry.putPresenterAndView(this, presenter);
            return view;
        });
    }


    /**
     * 船舰菜单选项
     * @return NavigationDrawer.Item
     */
    public NavigationDrawer.Item getMenuItem() {
        if (this.menuItem == null) {
            this.menuItem = new NavigationDrawer.ViewItem(this.getTitle(), menuIconNode, this.id, this.defaultViewStackPolicy);
        }



        return this.menuItem;
    }

    public void selectMenuItem() {
        this.menuItem.setSelected(true);
    }
//
//
//    public Optional<Object> switchView() {
//        ViewStackPolicy viewStackPolicy = (ViewStackPolicy) this.registry.getView(AppManager.getInstance().getView()).map((appView) -> {
//            return appView.defaultViewStackPolicy;
//        }).orElse(ViewStackPolicy.USE);
//        AppManager.getInstance().switchView(this.id, viewStackPolicy);
//    }
//    public Object switchView() {
//        AppManager.getInstance().switchView(getId(), getViewStackPolicy());
//        return this.getPresenter();
//
//    }

//    public Optional<Object> switchView(ViewStackPolicy viewStackPolicy) {
//        AppManager.getInstance().switchView(this.id, viewStackPolicy);
//        return this.getPresenter();
//    }


}
