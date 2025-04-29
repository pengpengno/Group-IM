package com.github.im.group.gui.controller.desktop.chat.messagearea.richtext.file;

import com.github.im.group.gui.controller.desktop.chat.messagearea.richtext.FileResource;
import com.github.im.group.gui.util.FileIconUtil;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import lombok.Getter;

import java.awt.*;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 展示文件资源的节点，包含文件图标、名称和大小。
 */
public class FileDisplay implements FileResource {

    @Getter
    private final FileInfo fileInfo;



    public FileDisplay(FileInfo fileInfo) {
        this.fileInfo = fileInfo;
    }

    private long initFileSize(Path path) {
        try {
            return Files.size(path);
        } catch (Exception e) {
            return 0;
        }
    }

    @Override
    public byte[] getBytes() {
        try {
            return fileInfo.getFileResource().getContentAsByteArray();
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public boolean isReal() {
        return true;
    }

    @Override
    public String getFilePath() {
        return fileInfo.getPath();
    }

    @Override
    public Node createNode() {
        String fileName ="";
        Long fileSize =0L;

        fileName = fileInfo.getName();
        fileSize = fileInfo.getSize();

        Label nameLabel = new Label(fileName);
        nameLabel.setStyle("-fx-font-weight: bold;");

        Label sizeLabel = new Label(formatSize(fileSize));

        sizeLabel.setStyle("-fx-text-fill: gray; -fx-font-size: 10px;");

        HBox container ;
        if (fileInfo instanceof  LocalFileInfo localFileInfo){
            File file = localFileInfo.getFile();
            ImageView icon = new ImageView(FileIconUtil.getFileIcon(file));
            icon.setFitWidth(32);
            icon.setFitHeight(32);



            container = new HBox(8, icon, nameLabel, sizeLabel);

            // 双击打开文件
            container.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2) {
                    try {
                        java.awt.Desktop.getDesktop().open(file);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });

            // 右键菜单
            ContextMenu contextMenu = new ContextMenu();

            MenuItem openItem = new MenuItem("打开文件");
            openItem.setOnAction(e -> {
                try {
                    if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
                        Desktop.getDesktop().open(file);
                    } else {
                        // 可以提示用户不支持打开文件 或者 fallback 到其他方式
                        System.out.println("当前环境不支持打开文件");
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            });

            MenuItem openFolderItem = new MenuItem("打开所在目录");
            openFolderItem.setOnAction(e -> {
                try {
                    if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
                        Desktop.getDesktop().open(file.getParentFile());
                    } else {
                        // 可以提示用户不支持打开文件 或者 fallback 到其他方式
                        System.out.println("当前环境不支持打开文件");
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            });

            contextMenu.getItems().addAll(openItem, openFolderItem);
            container.setOnContextMenuRequested(event -> contextMenu.show(container, event.getScreenX(), event.getScreenY()));

            // 拖拽支持（将文件拖出）
            container.setOnDragDetected(event -> {
                javafx.scene.input.Dragboard db = container.startDragAndDrop(javafx.scene.input.TransferMode.COPY);
                javafx.scene.input.ClipboardContent content = new javafx.scene.input.ClipboardContent();
                content.putFiles(java.util.Collections.singletonList(file));
                db.setContent(content);
                event.consume();
            });
        }else{
            container = new HBox(8, nameLabel, sizeLabel);
        }

        container.setPadding(new Insets(5));
        HBox.setHgrow(nameLabel, Priority.ALWAYS);


        return container;
    }


    private String formatSize(long size) {
        if (size >= 1024 * 1024) return String.format("%.1f MB", size / 1024.0 / 1024);
        if (size >= 1024) return String.format("%.1f KB", size / 1024.0);
        return size + " B";
    }
}
