//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.github.im.group.gui.views;

import com.github.im.group.gui.controller.PlatformView;
import com.github.im.group.gui.util.FxView;
import com.gluonhq.charm.glisten.mvc.View;
import com.gluonhq.charm.glisten.visual.MaterialDesignIcon;
import io.netty.util.internal.StringUtil;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public  class ViewRegistry {
//    private static final String VIEW_NAME = "gls-view-name";
//    private final Map<String, FView> viewMap = new ConcurrentHashMap<>();
    private final Map<String, ViewLifeCycle> viewMap = new ConcurrentHashMap<>();
    private final Map<String, Object> presenterMap = new ConcurrentHashMap<>();

    private static final String FXML_PATH  = "fxml/";
    private static final String FXML_SUFFIX = ".fxml";

    public enum Singleton {
        INSTANCE;

        private final ViewRegistry instance;

        Singleton() {
            instance = new ViewRegistry();
        }

        public ViewRegistry get() {
            return instance;
        }
    }

    public static ViewRegistry getInstance() {
        return Singleton.INSTANCE.get();
    }

    /**
     * 用于创建那些
     * @param presenterClass
     * @param menuIcon
     * @param flags
     * @return
     */
    public final FView createView(Class<?> presenterClass, MaterialDesignIcon menuIcon, FView.Flag... flags) {
        FView view = new FView( presenterClass, menuIcon, flags);
        this.viewMap.put(view.getId(), view);
        view.registry = this ;
        return view;
    }


    void putPresenterAndView(ViewLifeCycle view, Object presenter) {
        this.viewMap.put(view.getId(), view);
        this.presenterMap.put(view.getId(), presenter);
    }

    public Optional<Object> getPresenter(FView view) {
        return Optional.ofNullable(this.presenterMap.get(view.getId()));
    }

    public Collection<ViewLifeCycle> getViews() {
        return Collections.unmodifiableCollection(this.viewMap.values());
    }

    public Optional<ViewLifeCycle> getView(View view) {
        return getView(view.getId());
//        return Optional.ofNullable((FView)
//                this.viewMap.get(view.getProperties().get("gls-view-name")));
    }

    public Optional<ViewLifeCycle> getView(String id) {
        if (id != null && !id.isEmpty()) {
            var v = viewMap.get(id);
            return Optional.of(v);
        } else {
            return Optional.empty();
        }
    }




    /**
     * 构建平台特定的 FXML 文件路径
     *
     * 此方法通过检查提供的视图类是否具有 {@link FxView} 注解来确定 FXML 文件的路径
     * 如果注解存在，它会优先使用注解中指定的路径，然后是 FXML 文件名
     * 如果注解不存在或相关路径信息未指定，它会使用视图类的简单名称来构建默认路径
     * 如： MyView.class  ， 那么检索后就是返回 fxml/MyView.fxml
     *
     * @param viewclass 视图类，用于确定 FXML 文件的路径
     * @return FXML 文件的路径
     */
    public static Path buildFxmlPath(Class<? extends PlatformView> viewclass) {

        // 检查视图类是否具有 FxView 注解
        var annotation = viewclass.getAnnotation(FxView.class);
        if(annotation != null ){
            // 如果注解存在且指定了路径，直接返回该路径
            var path = annotation.path();
            if(!StringUtil.isNullOrEmpty(path)){
                return Paths.get(path);
            }
            // 如果注解存在且指定了 FXML 文件名，构建并返回对应的路径
            if(!StringUtil.isNullOrEmpty(annotation.fxmlName())){
                var fxmlName = annotation.fxmlName();
                return Paths.get(FXML_PATH + fxmlName + FXML_SUFFIX);
            }
        }
        // 如果没有注解或者注解中没有指定必要的信息，使用视图类的简单名称构建默认路径并返回
        return Paths.get(FXML_PATH + viewclass.getSimpleName() + FXML_SUFFIX);

    }



}
