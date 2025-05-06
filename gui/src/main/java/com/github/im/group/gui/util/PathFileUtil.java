package com.github.im.group.gui.util;

import com.github.im.common.connect.model.proto.Chat;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Slf4j
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


    public static Chat.MessageType getMessageType(@NotNull String fileName) {
        if(fileName == null){
            return Chat.MessageType.FILE;
        }
        fileName = fileName.toLowerCase();
        if (fileName.endsWith(".png") || fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")) {
            return Chat.MessageType.IMAGE;
        }
//        else if (fileName.endsWith(".mp4")) {
//            return Chat.MessageType.VIDEO;
//        } else if (fileName.endsWith(".txt")) {
//            return Chat.MessageType.TEXT;
//        } else if (fileName.endsWith(".md")) {
//            return Chat.MessageType.MARKDOWN;
//        }
        else {
            return Chat.MessageType.FILE;
        }
    }

    public static Path resolveUniqueFilename(Path directory, String originalFilename) {
        String name = originalFilename;
        String baseName = name;
        String extension = "";

        int dotIndex = name.lastIndexOf('.');
        if (dotIndex != -1) {
            baseName = name.substring(0, dotIndex);
            extension = name.substring(dotIndex); // includes dot
        }

        Path path = directory.resolve(name);
        int counter = 1;

        while (Files.exists(path)) {
            name = baseName + "(" + counter + ")" + extension;
            path = directory.resolve(name);
            counter++;
        }

        try {
            Files.createDirectories(directory); // 确保目录存在
        } catch (IOException e) {
            log.warn("创建目录失败: {}", directory, e);
        }

        return path;
    }

}
