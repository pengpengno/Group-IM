package com.github.im.group.gui.util;

import io.github.palexdev.mfxcore.utils.fx.SwingFXUtils;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import org.springframework.core.io.ClassPathResource;

import javax.swing.*;
import javax.swing.filechooser.FileSystemView;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

public class FileIconUtil {

    private static final ClassPathResource iconResource = new ClassPathResource("images/icon.png");
    public static void setStageIcon (Stage stage) {
        if (stage == null ){
            return ;
        }

        if (iconResource.exists()){
            try {
                stage.getIcons().add(new Image(iconResource.getInputStream()));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

    }

    /**
     *  获取系统文件的图标
     *  默认为 80*80
     * @param file
     * @return 返回系统图标 ，没有的就返回默认的文件图标
     */
    public static Image getFileIcon(File file) {
        try {
            var fileSystemView = FileSystemView.getFileSystemView();

            var icon = fileSystemView.getSystemIcon(file, 80, 80);

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
