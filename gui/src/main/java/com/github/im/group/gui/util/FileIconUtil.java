package com.github.im.group.gui.util;

import io.github.palexdev.mfxcore.utils.fx.SwingFXUtils;
import javafx.scene.image.Image;

import javax.swing.*;
import javax.swing.filechooser.FileSystemView;
import java.awt.image.BufferedImage;
import java.io.File;

public class FileIconUtil {

    public static Image getFileIcon(File file) {
        try {
            Icon icon = FileSystemView.getFileSystemView().getSystemIcon(file);
            BufferedImage bufferedImage = new BufferedImage(
                    icon.getIconWidth(),
                    icon.getIconHeight(),
                    BufferedImage.TYPE_INT_ARGB
            );
            icon.paintIcon(null, bufferedImage.getGraphics(), 0, 0);
            return SwingFXUtils.toFXImage(bufferedImage, null);
        } catch (Exception e) {
            return new Image("/icons/default_file_icon.png"); // fallback
        }
    }
}
