package com.github.im.group.gui.views;/**
 * Description: [描述方法或类的作用和功能]
 * <p>
 * </p>
 *
 * @author [peng]
 * @since 27
 */

import com.github.im.group.gui.util.PlatformUtils;
import javafx.application.Platform;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Description:
 * <p>
 *     用以 设计不用平台的ui 加载
 * </p>
 *
 * @author pengpeng
 * @version 1.0
 * @since 2025/5/27
 */
public interface PlatformUI {
    // 日志 logger
    public static final Logger logger = LoggerFactory.getLogger(PlatformUI.class);


    /**
     *
     * 在注册的时候 也初始化 ui {@link com.github.im.group.gui.controller.RegisterView#registerView()}
     * 加载ui,不要抛出异常
     */
    default void loadUi() {
        //平台判断
        Platform.runLater(() -> {
            try{
                if (PlatformUtils.isDesktop()) {
                    desktop();
                } else {
                    mobile();
                }
            }catch (Exception e){
                logger.error("加载ui异常",e);
            }
        });
    }

    /**
     * 移动端 ui 加载
     */
    public void desktop();


    /**
     * 移动端 ui 加载
     */
    public void mobile();

}
