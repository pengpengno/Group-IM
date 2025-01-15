package com.github.im.group.gui.util;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import java.io.File;
import java.net.URL;
import java.util.concurrent.ConcurrentHashMap;


/***
 *  所有 fxml 设计为 与controller 同名 且 同路径
 */
@Component
@Slf4j
public class FxmlLoader implements ApplicationContextAware {

    private final static ConcurrentHashMap<Class<?>,Stage> stageMap = new ConcurrentHashMap<>();

    private final static ConcurrentHashMap<Class<?>,Scene> sceneMap = new ConcurrentHashMap<>();


    private final static String FXML_SUFFIX = ".fxml";

    public final static String FXML_PATH_FLODER = "/fxml";

    private static ApplicationContext applicationContext;



    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }


    public static URL  getFxmlResourceUrl(String name) {
        URL fxmlResourceUrl = null;
        if (!StringUtils.endsWithIgnoreCase(name,FXML_SUFFIX)){
            name = name + FXML_SUFFIX;
        }

        if (!StringUtils.startsWithIgnoreCase(name, File.separator)){
            name = File.separator + name ;
        }
        var fxmlResource = new ClassPathResource(FXML_PATH_FLODER + name);

        if(!fxmlResource.exists()){

            fxmlResource = new ClassPathResource(name);

        }
        try{

            fxmlResourceUrl = fxmlResource.getURL();
            return fxmlResourceUrl;
        }catch (Exception exception){
            log.error("Failed to load FXML resource for  {}",  exception.getMessage(), exception);
        }

        throw new RuntimeException("Failed to load FXML resource for " );

    }

    public static URL  getFxmlResourceUrl(Class<?> clazz) {
        try {
            var viewAnn = clazz.getAnnotation(FxView.class);

            var  path = "";
            URL fxmlResourceUrl = null;

            if(viewAnn != null ){
                path = viewAnn.path();

                fxmlResourceUrl = getFxmlResourceUrl(path);
            }
            return fxmlResourceUrl;
        }catch (Exception exception){
            log.error("Failed to load FXML resource for {}: {}", clazz.getName(), exception.getMessage(), exception);
        }

        throw new RuntimeException("Failed to load FXML resource for " + clazz.getName());
    }

    /***
     * 获取单例的 Stage
     * 此方法会有三种方式来搜索对应的fxml 文件
     * <ul>
     *     <li>1. {@link  FxView#path()}  fx 窗口路径} 根据路径在 {@link FxmlLoader#FXML_PATH_FLODER 默认fxml目录}查询</li>
     *     <li>2. {@link  FxView#path()}  fx 窗口路径} 根据 注解中的路径直接查询</li>
     *     <li>3. 根据指定class 查询其同级目录同级名称的fxml</li>
     * </ul>
     * @param clazz 传入的类
     * @return 返回加载的 stage
     */
    private static Scene applySingleScene(Class<?> clazz) {
        try {

            Assert.notNull(clazz, "The specified class cannot be null!");

            var fxmlResourceUrl = getFxmlResourceUrl(clazz);

            if (fxmlResourceUrl == null){
                // 如果不存在则直接 使用类的路径来查询
                clazz.getResource(buildClassFxmlPath(clazz));
            }

            Assert.notNull(fxmlResourceUrl, "FXML file not found for the specified class!");

            FXMLLoader fxmlLoader = new FXMLLoader(fxmlResourceUrl);
            fxmlLoader.setControllerFactory(applicationContext::getBean);

            Parent load = fxmlLoader.load();

            var scene = new Scene(load);

            return scene;

        } catch (Exception e) {
            log.error("Failed to create scene for {}: {}", clazz.getName(), e.getMessage(), e);
            return null;
        }
    }



    /***
     * Builds the FXML path string based on the given class.
     * @param clazz The class for which to build the FXML path.
     * @return The constructed FXML path.
     */
    private static String buildClassFxmlPath(Class<?> clazz) {
        return clazz.getSimpleName() + FXML_SUFFIX;
    }

    /**
     * Loads a singleton Stage.
     * @param clazz The class for which to load the stage.
     * @return The loaded Stage.
     */
    public static Stage applySingleStage(Class<?> clazz) {
        return stageMap.computeIfAbsent(clazz, key -> {
            Stage stage = new Stage();
            Scene scene = applySingleScene(clazz);
            if (scene != null) {
                stage.setScene(scene);
                addCloseEventHandler(stage, clazz);
            }
            return stage;
        });
    }

    /**
     * Loads a prototype Stage (a new instance each time).
     * @param clazz The class for which to load the stage.
     * @return The newly created Stage.
     */
    public Stage applyMultiStage(Class<?> clazz) {
        Stage stage = new Stage();
        Scene scene = applySingleScene(clazz);
        if (scene != null) {
            stage.setScene(scene);
            addCloseEventHandler(stage, clazz);
        }
        return stage;
    }

    /**
     * Adds a close event handler to the given stage.
     * @param stage The stage to which the event handler is added.
     * @param clazz The class associated with the stage.
     */
    private static void addCloseEventHandler(Stage stage, Class<?> clazz) {
        stage.addEventHandler(WindowEvent.WINDOW_CLOSE_REQUEST, evt -> {
            log.debug("Closing stage for class: {}", clazz.getName());
            stageMap.remove(clazz);
            sceneMap.remove(clazz);
        });
    }

    /**
     * 获取 相应的实例
     * Retrieves a cached scene if it exists.
     * @param clazz The class for which to retrieve the scene.
     * @return The cached Scene, or null if not found.
     */
    public static Scene getSceneInstance(Class<?> clazz) {
        return sceneMap.computeIfAbsent(clazz,FxmlLoader::applySingleScene);
    }

    /**
     * Clears all cached stages and scenes. This can be used when you need to forcefully reload.
     */
    public void clearAll() {
        stageMap.clear();
        sceneMap.clear();
    }

}
