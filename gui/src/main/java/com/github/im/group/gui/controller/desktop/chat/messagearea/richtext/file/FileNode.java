package com.github.im.group.gui.controller.desktop.chat.messagearea.richtext.file;

import cn.hutool.core.io.FileUtil;
import com.github.im.dto.session.MessageDTO;
import com.github.im.group.gui.controller.desktop.MessageWrapper;
import com.github.im.group.gui.controller.desktop.chat.messagearea.richtext.MessageNode;
import com.github.im.group.gui.util.FileIconUtil;
import com.github.im.group.gui.util.PathFileUtil;
import io.github.palexdev.materialfx.controls.MFXButton;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import lombok.Getter;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.awt.*;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.ResourceBundle;

/**
 * 展示文件资源的节点，包含文件图标、名称和大小。
 */
@Component
@Scope("prototype")
@Slf4j
public class FileNode implements MessageNode {

    static final ResourceBundle menuBundle = ResourceBundle.getBundle("i18n.menu.button");

    @Getter
    private final FileInfo fileInfo;

    private boolean  exists ; // 文件是否存在

    MessageWrapper messageWrapper;
    private RemoteFileService remoteFileService;


    public FileNode(FileInfo fileInfo) {
        this.fileInfo = fileInfo;
    }

    private long initFileSize(Path path) {
        try {
            return Files.size(path);
        } catch (Exception e) {
            return 0;
        }
    }

    public String getDescription () {
        return fileInfo.getName();
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
    public String getFilePath() {
        return fileInfo.getPath();
    }

    @Override
    public Node createNode() {

//        var content = messageWrapper.getContent();
        String fileName ="";
        Long fileSize =0L;

        fileName = fileInfo.getName();
        fileSize = fileInfo.getSize();

        Label nameLabel = new Label(fileName);
        nameLabel.setStyle("-fx-font-weight: bold;");

        Label sizeLabel = new Label(formatSize(fileSize));

        sizeLabel.setStyle("-fx-text-fill: gray; -fx-font-size: 10px;");

        //TODO 文件下载
        /**
         * 1. 先 获取文件 id  判断当前文件是否已经存在当前磁盘
         *  1.1 根据文件名称再文件夹路径下匹配 ，匹配到相同文件名 则比对其hash 值？ 但是这样重名文件的判断会比较麻烦 ，
         *  因为下载时同名文件的命名规则会处理 成xxx(1)
         */
        HBox container ;
        HBox.setHgrow(nameLabel, Priority.ALWAYS);

        if (fileInfo instanceof  LocalFileInfo localFileInfo){
            File file = localFileInfo.getFile();
            ImageView icon = new ImageView(FileIconUtil.getFileIcon(file));
            icon.setFitWidth(40);
            icon.setFitHeight(40);

            container = new HBox(8, icon, nameLabel, sizeLabel);

            // 双击打开文件
            container.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2) {
                    try {
                        // gluon 环境中使用 好像有点问题
                        Desktop.getDesktop().open(file);
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
            return container;

        }else if (fileInfo instanceof  RemoteFileInfo remoteFileInfo){

            container = new HBox(8, nameLabel, sizeLabel);
            ImageView buttonIcon = new ImageView();
            buttonIcon.setFitWidth(25);
            buttonIcon.setFitHeight(25);

            var icon  = new Image(Objects.requireNonNull(getClass().
                    getResourceAsStream("/" + menuBundle.getString("download.icon"))));
            buttonIcon.setImage(icon);
            var button = new MFXButton();

            button.setAlignment(Pos.TOP_CENTER);

            button.setGraphic(buttonIcon);
            button.setText(null);
            button.setMaxSize(50, 50);
            button.setPrefSize(50, 50);
            button.setPadding(new Insets(0, 0, 0, 0));

            // 增加个下载的按钮   展示
            // 点击按钮下载文件
            String finalFileName = fileName;
            button.setOnMouseClicked(e -> {
                remoteFileService.download(remoteFileInfo)
                        .blockOptional()
                        .ifPresent(resource -> {
                            try {
                                Path downloadDir = Paths.get("downloads");
                                Files.createDirectories(downloadDir); // 确保目录存在
                                Path targetPath = PathFileUtil.resolveUniqueFilename(downloadDir, finalFileName);
                                Files.write(targetPath, resource.getContentAsByteArray());

                                File localFile = targetPath.toFile();

//                                // 替换界面元素（假设 container 是外部父节点）
//                                var parent = container.getParent();
//                                if (parent instanceof HBox parentBox) {
//                                    int index = parentBox.getChildren().indexOf(container);
//                                    parentBox.getChildren().set(index, newNode);
//                                }
                                // 去除就行
                                container.getChildren().removeAll(button);
                                ImageView localFileIcon = new ImageView(FileIconUtil.getFileIcon(localFile));
                                localFileIcon.setFitWidth(40);
                                localFileIcon.setFitHeight(40);
                                container.getChildren().add(0, localFileIcon);

                            } catch (Exception ex) {
                                log.error("下载或保存文件失败", ex);
                            }
                        });
            });



            return container ;
        }
        container = new HBox(8, nameLabel, sizeLabel);



        return container;
    }


    private String formatSize(long size) {
        if (size >= 1024 * 1024) return String.format("%.1f MB", size / 1024.0 / 1024);
        if (size >= 1024) return String.format("%.1f KB", size / 1024.0);
        return size + " B";
    }
}
