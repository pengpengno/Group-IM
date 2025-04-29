package com.github.im.group.gui.util;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

public class PathFileUtil {

    /**
     * 将 Path 转换为 File
     */
    public static File toFile(Path path) {
        return path != null ? path.toFile() : null;
    }

    /**
     * 将 File 转换为 Path
     */
    public static Path toPath(File file) {
        return file != null ? file.toPath() : null;
    }

    /**
     * 获取 Path 的绝对路径字符串
     */
    public static String toAbsolutePath(Path path) {
        return path != null ? path.toAbsolutePath().toString() : null;
    }

    /**
     * 获取 File 的绝对路径字符串
     */
    public static String toAbsolutePath(File file) {
        return file != null ? file.getAbsolutePath() : null;
    }

    /**
     * 从字符串创建 Path
     */
    public static Path fromString(String pathStr) {
        return pathStr != null ? Paths.get(pathStr) : null;
    }
}
