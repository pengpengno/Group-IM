package com.github.im.common.connect.module;

import com.google.inject.Guice;
import com.google.inject.Injector;

/**
 * 基于 Guice 的 Module 注入
 */
public class GuiceModuleInjector {

    public static Injector injector = Guice.createInjector(new ConnectionModule());

    public static <T> T getInstance(Class<T> clazz) {
        return injector.getInstance(clazz);
    }



}
