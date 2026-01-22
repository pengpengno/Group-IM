package com.github.im.common.connect.enums;

import com.github.im.common.connect.model.proto.User;

/**
     * DEFAULT IS DESKTOP
     */
public enum PlatformType {
    DESKTOP,

    MOBILE,
//    ANDROID,
//    IOS,
//    WEB,
//    WINDOWS,
//    MAC,
//    LINUX,
//    UNKNOWN

    ;

    enum OsSystem {
        ANDROID,
        IOS,
        WINDOWS,
        MAC,
        LINUX,
        UNKNOWN,
        WEB,
        ;

    }


    public static PlatformType getPlatformType(User.PlatformType platform) {
        return switch (platform) {
            case ANDROID -> MOBILE;
            case IOS -> MOBILE;
            case WEB -> DESKTOP;
            case WINDOWS -> DESKTOP;
            case MAC -> DESKTOP;
            case LINUX -> DESKTOP;
            default -> DESKTOP;
        };

    }



}




