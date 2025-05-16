package com.github.im.group.gui.controller;/**
 * Description: [描述方法或类的作用和功能]
 * <p>
 * </p>
 *
 * @author [peng]
 * @since 12
 */

import com.github.im.common.connect.enums.PlatformType;
import com.github.im.common.connect.model.proto.Account;
import com.gluonhq.attach.util.Platform;

import static com.github.im.common.connect.enums.PlatformType.*;

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

    public static PlatformType  DEFAULT_PLATFORM = DESKTOP;


    default String getName() {
        return this.getClass().getName();
    }
    public PlatformType getPlatform();




    public static PlatformType getCurrentPlatform() {
        return getPlatformType(Platform.getCurrent());
    }

    public static Account.PlatformType getCurrentPlatformType() {


        var current = Platform.getCurrent();

        switch (current) {
            case ANDROID -> {
                return Account.PlatformType.ANDROID;
            }
            case IOS -> {
                return Account.PlatformType.IOS;
            }
            default -> {

                var osName = System.getProperty("os.name").toLowerCase();

                if (osName.contains("win")) {
                    return Account.PlatformType.WINDOWS;
                } else if (osName.contains("mac")) {
                    return Account.PlatformType.IOS;
                } else if (osName.contains("nix") || osName.contains("nux") || osName.contains("aix")) {
                    return Account.PlatformType.LINUX;
                } else if (osName.contains("android")) {
                    return Account.PlatformType.ANDROID;
                } else {
                    return Account.PlatformType.WINDOWS;
                }
            }
        }
    }

    //  enum corresponding to the  gluon Platform enum
    public static PlatformType getPlatformType(Platform platform) {
        return switch (platform) {
            case DESKTOP -> DESKTOP;
            case ANDROID -> MOBILE;
            case IOS -> MOBILE;
            default -> DESKTOP;
        };
    }
}
