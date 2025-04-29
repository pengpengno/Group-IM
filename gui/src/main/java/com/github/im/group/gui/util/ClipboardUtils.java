package com.github.im.group.gui.util;

import javafx.scene.image.Image;
import javafx.scene.input.Clipboard;

import java.io.File;
import java.util.List;

public class ClipboardUtils {

    /**
     * 从系统剪贴板获取图像
     *
     * 此方法尝试从系统剪贴板中提取图像数据如果剪贴板中包含图像数据，则直接返回该图像数据；
     * 否则，返回null此方法主要用于需要从剪贴板获取图像的应用场景，例如截图粘贴、图像数据共享等
     *
     * @return Image 如果剪贴板中包含图像数据，则返回图像对象；否则返回null
     */
    public static Image getImageFromClipboard() {
        // 获取系统剪贴板实例
        Clipboard clipboard = Clipboard.getSystemClipboard();
        // 检查剪贴板中是否包含图像数据
        if (clipboard.hasImage()) {
            // 如果包含图像数据，则返回剪贴板中的图像数据
            return clipboard.getImage();
        }
        // 如果不包含图像数据，则返回null
        return null;
    }

    /**
     * 从剪切板获取文件列表
     * 此方法用于获取当前剪切板中的文件列表，如果剪切板中没有文件，则返回一个空列表
     * 主要用途是快速从剪切板中提取出用户可能刚刚剪切或复制的文件集合，以便进行进一步的处理
     *
     * @return 如果剪切板中有文件，则返回包含这些文件的列表；否则返回空列表
     */
    public static List<File> getFilesFromClipboard() {
        // 获取系统剪切板
        Clipboard clipboard = Clipboard.getSystemClipboard();

        // 检查剪切板中是否含有文件
        if (clipboard.hasFiles()) {
            // 返回剪贴板中的文件列表
            return clipboard.getFiles();
        }

        // 否则返回空列表
        return List.of();
    }
}
