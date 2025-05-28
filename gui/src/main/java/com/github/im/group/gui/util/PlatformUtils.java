package com.github.im.group.gui.util;

import com.github.im.common.connect.enums.PlatformType;
import com.gluonhq.attach.util.Platform;

import static com.github.im.common.connect.enums.PlatformType.*;

/**
 * Description:
 * <p>
 * </p>
 *
 * @author pengpeng
 * @version 1.0
 * @since 2025/5/27
 */
public class PlatformUtils {

    public static boolean isDesktop(){
        return Platform.isDesktop();
    }
    public static PlatformType getCurrentPlatform() {
        return getPlatformType(Platform.getCurrent());
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