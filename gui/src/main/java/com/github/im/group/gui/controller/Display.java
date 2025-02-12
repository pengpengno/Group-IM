package com.github.im.group.gui.controller;/**
 * Description: [描述方法或类的作用和功能]
 * <p>
 * </p>
 *
 * @author [peng]
 * @since 06
 */

import com.github.im.group.gui.util.FxView;
import com.github.im.group.gui.util.FxmlLoader;
import com.github.im.group.gui.util.StageManager;
import com.gluonhq.charm.glisten.application.AppManager;
import com.gluonhq.charm.glisten.mvc.View;
import javafx.application.Platform;
import javafx.scene.Scene;
import lombok.extern.slf4j.Slf4j;
import org.kordamp.bootstrapfx.BootstrapFX;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Description:
 * <p>
 *     display windows with corresponding platform fxml
 * </p>
 *
 * @author pengpeng
 * @version 1.0
 * @since 2024/11/6
 */
@Component
@Slf4j
public class Display  implements ApplicationContextAware {

    private static ApplicationContext applicationContext;



    private static final String DESKTOP_PATH = "desktop/";
    private static final String FXML_PATH  = "fxml/";
    private static final String MOBILE_PATH = "mobile/";
    private static final String FXML_SUFFIX = ".fxml";

    public static final ConcurrentHashMap<String,View> DISPLAY_VIEW_MAP = new ConcurrentHashMap<>();

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }



    /**
     * 展示当前窗体
     * 此处使用的事 Gluon 的组件来渲染 窗体
     * @param displayClass
     */
    public static  void display(Class<? extends PlatformView> displayClass){
        Platform.runLater(()-> {
            var appManager = AppManager.getInstance();

            var annotation = displayClass.getAnnotation(FxView.class);

            var currentPlatform = com.gluonhq.attach.util.Platform.getCurrent();



            //根据当前平台调用不同的 view
            var platformType = PlatformView.PlatformType.getPlatformType(currentPlatform);

            log.debug("currentPlatform is {} ,platformType {} ",currentPlatform,platformType);


            Map<String, ? extends PlatformView> beansOfType = applicationContext.getBeansOfType(displayClass);

            if(beansOfType.isEmpty()){
                log.info("");
                return;
            }
            Collection<? extends PlatformView> beans = beansOfType.values();
            var platform = beans.stream().filter(platformView -> {
                return platformType.equals(platformView.getPlatform());
            }).findFirst().map(PlatformView::getPlatform).orElseGet(() -> PlatformView.DEFAULT_PLATFORM);

            if (annotation != null){
                var viewName = displayClass.getName();
                var cached = DISPLAY_VIEW_MAP.containsKey(viewName);
                if (!cached){
                    DISPLAY_VIEW_MAP.computeIfAbsent(viewName,key->{
                        String fxmlLoaderPath ;
                        if (platform.equals(PlatformView.PlatformType.DESKTOP)){
                            fxmlLoaderPath = FXML_PATH+DESKTOP_PATH+annotation.fxmlName()+FXML_SUFFIX;
                        }else{
                            fxmlLoaderPath = FXML_PATH+MOBILE_PATH+annotation.fxmlName()+FXML_SUFFIX;

                        }

                        var PARENT = FxmlLoader.loadFxml(fxmlLoaderPath);
                        View view ;
                        if (PARENT != null){

                            view =  new View(PARENT);
                        }
                        else{
                            view = new View();
                        }
                        try{
                            appManager.addViewFactory(viewName,()->{
                                return view;
                            });
                        }catch (Exception exception){
                            exception.printStackTrace();
                        }

                        return view;
                    });

                }



//                try{
//                    appManager.addViewFactory(viewName,()->{
////                    appManager.addViewFactory(displayClass.getName(),()->{
//                        var sceneInstance = FxmlLoader.getSceneInstance(displayClass);
//                        if (sceneInstance != null){
//                            return new View(sceneInstance.getRoot());
//                        }
//                        return new View();
//                    });
//                }catch (Exception exception){
//                    exception.printStackTrace();
////                    log.error("already existed ",exception);
//                }

                appManager.switchView(viewName);
            }

        });


    }

    /**
     * 展示当前窗体
     * 此方法使用的事原始的 Javafx 在 primaryStage 切换 Scene ，在
     * @param displayClass
     */
    @Deprecated
    public static  void displayV1(Class<?> displayClass){

        Platform.runLater(()-> {
//            Class<? extends Display> displayClass = this.getClass();
            var scene = FxmlLoader.getSceneInstance(displayClass);
            var primaryStage = StageManager.getPrimaryStage();
            primaryStage.sizeToScene(); // 自动调整主 Stage 大小以适应当前 Scene 的大小

//            scene.getStylesheets().add(BootstrapFX.bootstrapFXStylesheet());       //(3)

            scene.widthProperty().addListener((observable, oldValue, newValue) -> {
                primaryStage.setWidth(newValue.doubleValue());
            });
            scene.heightProperty().addListener((observable, oldValue, newValue) -> {
                primaryStage.setHeight(newValue.doubleValue());
            });


            primaryStage.setResizable(true);
            primaryStage.setScene(scene);
        });
    }
}
