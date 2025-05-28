package com.github.im.group.gui.views;

import com.github.im.group.gui.controller.desktop.chat.ChatMainPresenter;
import com.gluonhq.charm.glisten.application.AppManager;
import com.gluonhq.charm.glisten.application.ViewStackPolicy;
import com.gluonhq.charm.glisten.control.NavigationDrawer;
import com.gluonhq.charm.glisten.mvc.View;
import com.gluonhq.charm.glisten.visual.MaterialDesignIcon;
import javafx.scene.Node;

import java.util.Arrays;

/**
 * Description: bean View
 * <p>
 *     用于将 bean 组件注册为 view ,这样的 bean class 如{@link ChatMainPresenter 会话聊天}必须继承 {@link View}
 * </p>
 *
 * @author pengpeng
 * @version 1.0
 * @since 2025/5/16
 */
public class BView implements ViewLifeCycle {

    private final Object presenter;

    private final String viewName;

    private ViewStackPolicy  viewStackPolicy = null;

    private final ViewRegistry registry = ViewRegistry.getInstance();

    @Override
    public ViewStackPolicy getViewStackPolicy() {
        if (viewStackPolicy == null ){
            return ViewStackPolicy.SKIP;
        }



        return viewStackPolicy;
    }

    private NavigationDrawer.Item item;

    @Override
    public String getId() {
        return viewName;
    }

    /**
     * 不是 View 的子类直接抛出异常  实体必须继承 {@link View }
     * @param presenter 组件 bean 对象
     * @throws IllegalArgumentException 抛出
     */
    public BView(Object presenter) throws  IllegalArgumentException {
        if (presenter instanceof View) {
            this.presenter = presenter;
            viewName = presenter.getClass().getSimpleName();
        } else {
            throw new IllegalArgumentException("presenter must be instance of View");
        }
    }



    @Override
    public void registerView()  throws IllegalArgumentException{

        // 判断是否 是 View 的子类 , 不是的 就抛出异常

        AppManager.getInstance().addViewFactory(viewName, () -> {

            // 这里不要 new 出来view 存放 不然会出现两个view ,因为 spring 容器已经存在了该bean， 复用即可

            View view = (View) presenter;
            getRegistry().putPresenterAndView(this, presenter);

            /**
             * 加个判断 用于加载不同平台的界面 {@link PlatformUI 平台ui}
             */
            if(presenter instanceof PlatformUI  ui) {
                ui.loadUi();
            }
            return view;
        });

    }

    @Override
    public ViewRegistry getRegistry() {
        return ViewRegistry.getInstance();
    }

    @Override
    public NavigationDrawer.Item getMenuItem() {

        String title = presenter.getClass().getSimpleName();
        Node menuIconNode = MaterialDesignIcon.HOME.graphic();
        ViewStackPolicy defaultViewStackPolicy = ViewStackPolicy.SKIP;
        if (this.item == null) {
            /**
             * 一些属性的判断
             */
            if (presenter instanceof MenuItem) {
                // 如果是 menuItem 类型， 那么需要加载到侧边栏
                MenuItem menuItem = (MenuItem) presenter;
                title = menuItem.title();
                menuIconNode = menuItem.menuIcon();

                defaultViewStackPolicy = Arrays.stream(menuItem.flags()).toList()
                                        .contains(FView.Flag.SKIP_VIEW_STACK)
                                        ? ViewStackPolicy.SKIP : ViewStackPolicy.USE;
                viewStackPolicy = defaultViewStackPolicy;

            }


        }

        this.item = new NavigationDrawer.ViewItem(title, menuIconNode, getId(), defaultViewStackPolicy);

        return item ;

    }


    private ViewLifeCycle.Flag[] getFlags() {
        if (presenter instanceof MenuItem) {
            MenuItem menuItem = (MenuItem) presenter;
            return menuItem.flags();


        }
    return new ViewLifeCycle.Flag[]{
    };
    }

    @Override
    public boolean isShownInDrawer() {

        return Arrays.stream(getFlags()).toList().contains(FView.Flag.SHOW_IN_DRAWER);

    }

    @Override
    public Object getPresenter() {
        return presenter;
    }
}