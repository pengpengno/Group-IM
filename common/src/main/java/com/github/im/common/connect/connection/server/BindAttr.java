package com.github.im.common.connect.connection.server;

import com.github.im.common.connect.connection.ReactiveConnectionManager;
import com.github.im.common.connect.enums.PlatformType;
import com.github.im.common.connect.model.proto.Account;
import lombok.Getter;
import lombok.Setter;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class BindAttr<T> {

    @Getter
    @Setter
    T key;

    private static <T> BindAttr<T> getAttr(T key) {
        var attr = new BindAttr<T>();
        attr.setKey(key);
        return attr;
    }

    /**
     * 构造用于与 连接 绑定的对象
     *
     * @param account
     * @return BindAttr
     */
    public static BindAttr<String> getBindAttr(String account) {
        return getBindAttr(account,PlatformType.DESKTOP);
//        var ATTRKEY = String.join("_", account , ReactiveConnectionManager.ALL_PLATFORM_PUSH_TAG);
//        return getAttr(ATTRKEY);
    }


    /**
     * 构造用于与 进行推送的连接
     *
     * @param account
     * @return BindAttr
     */
    public static BindAttr<String> getBindAttrForPush(String account) {
//        return getBindAttr(account,PlatformType.DESKTOP);
        var ATTRKEY = String.join("_", account , ReactiveConnectionManager.ALL_PLATFORM_PUSH_TAG);
        return getAttr(ATTRKEY);
    }

    public static List<BindAttr<String>> getAllPlatformBindAttr(String account) {
        return Arrays.stream(PlatformType.values()).map(e->getBindAttr(account,e) ).collect(Collectors.toList());
    }


    public static BindAttr<String> getBindAttr(String account, PlatformType platformType ) {
        var ATTRKEY = String.join("_", account, platformType.name());
        return getAttr(ATTRKEY);
    }


    public static BindAttr<String> getBindAttr(Account.AccountInfo accountInfo) {
        var platformType = accountInfo.getPlatformType();
        var platformTypeEnums = PlatformType.getPlatformType(platformType);
        var account = accountInfo.getAccount();
        return getBindAttr(account,platformTypeEnums);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BindAttr<?> bindAttr = (BindAttr<?>) o;
        return key != null ? key.equals(bindAttr.key) : bindAttr.key == null;
    }

    @Override
    public int hashCode() {
        return key != null ? key.hashCode() : 0;
    }
}
