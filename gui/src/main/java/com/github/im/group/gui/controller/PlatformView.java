package com.github.im.group.gui.controller;/**
 * Description: [描述方法或类的作用和功能]
 * <p>
 * </p>
 *
 * @author [peng]
 * @since 12
 */

import com.gluonhq.attach.util.Platform;

/**
 * Description:
 * <p>
 * </p>
 *
 * @author pengpeng
 * @version 1.0
 * @since 2025/2/12
 */
public interface PlatformView {


    public static PlatformType  DEFAULT_PLATFORM = PlatformType.DESKTOP;


    public PlatformType getPlatform();


    /**
     * DEFAULT IS DESKTOP
     */
    public enum PlatformType {
        DESKTOP,
        ANDROID,
        IOS,
        WEB,
        UNKNOWN

        ;


        //  enum corresponding to the  gluon Platform enum
        public static PlatformType getPlatformType(Platform platform) {
            return switch (platform) {
                case DESKTOP -> DESKTOP;
                case ANDROID -> ANDROID;
                case IOS -> IOS;
                default -> UNKNOWN;
            };

        }
    }



}
