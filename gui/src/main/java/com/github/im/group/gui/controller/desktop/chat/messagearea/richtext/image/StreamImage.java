package com.github.im.group.gui.controller.desktop.chat.messagearea.richtext.image;

import com.github.im.common.connect.model.proto.Chat;
import com.github.im.group.gui.controller.desktop.chat.messagearea.richtext.MessageNode;
import com.github.im.group.gui.util.FileIconUtil;
import com.github.im.group.gui.util.ImageUtil;
import com.github.im.group.gui.util.PathFileUtil;
import javafx.scene.Node;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import lombok.Getter;

import java.io.IOException;
import java.nio.file.Path;
import java.util.UUID;


/**
 * A custom object which contains image file stream .
 * When rendered in the rich text editor, the image is loaded from the dataInputStream
 */
public class StreamImage implements MessageNode {

    @Getter
    private Image image;

    private String format;

    private byte[] imageData;

    private Path path;

    public StreamImage(Image image) {
        this.image = image;
    }


    public StreamImage(Path path) {
        this.path = path;
    }



    @Override
    public String getDescription() {
        if (path != null) {
            // 存在路径就 返回路径的文件名称
            return path.getFileName().toString();
        }
        return UUID.randomUUID().toString()+".png";
    }

    @Override
    public Chat.MessageType getType() {
        return Chat.MessageType.IMAGE;
    }

    @Override
    public String getFilePath() {
        if (path != null){
            return PathFileUtil.toAbsolutePath(path);
        }
        return "";
    }

    @Override
    public String toString() {
        return String.format("StreamImage[path=%s]", "");
    }


    @Override
    public byte[] getBytes() {
        if(imageData == null){
            try {
                imageData = ImageUtil.imageToBytes(image,"png");
            } catch (IOException e) {
                return null;
            }
        }
        return imageData;
    }

    @Override
    public Node createNode() {
        if (path != null ){
            image = new Image("file:" + path);
        }

        ImageView imageView = new ImageView(image);
//        imageView.setFitWidth(100);
//        imageView.setPreserveRatio(true);


        // 最大宽度或高度，避免图像过大
        double maxWidth = 300;
        double maxHeight = 300;

        double imageWidth = image.getWidth();
        double imageHeight = image.getHeight();

        // 缩放比例
        double widthRatio = maxWidth / imageWidth;
        double heightRatio = maxHeight / imageHeight;
        double scale = Math.min(1.0, Math.min(widthRatio, heightRatio));

        imageView.setFitWidth(imageWidth * scale);
        imageView.setFitHeight(imageHeight * scale);
        imageView.setPreserveRatio(true);

        imageView.setOnMouseClicked(event-> {
            //TODO  双击放大查看
            if (event.getClickCount() == 2) {
                showImageInNewWindow();
            }
        });

        return imageView;
    }


    /**
     * 在新窗口中显示图片
     * 此方法使用JavaFX创建一个新的窗口来展示图片，并允许用户通过滚轮缩放图片大小
     */
    private void showImageInNewWindow() {
        // 创建一个新的JavaFX窗口
        Stage stage = new javafx.stage.Stage();
        // 设置窗口标题
        stage.setTitle("查看图片");
        // 清除窗口图标，此处不需要显示窗口图标
        stage.getIcons().clear();
        FileIconUtil.setStageIcon(stage);

        // 创建一个ImageView对象来显示图片，image为预加载的图片对象
        ImageView fullImageView = new ImageView(image);
        // 保持图片的宽高比例
        fullImageView.setPreserveRatio(true);
        // 设置图片显示质量为高
        fullImageView.setSmooth(true);
        // 启用图片缓存以提高性能
        fullImageView.setCache(true);

        // 创建一个StackPane对象来作为窗口的根布局，并将ImageView添加到其中
        javafx.scene.layout.StackPane root = new javafx.scene.layout.StackPane(fullImageView);

        // 创建一个Scene对象，设置根布局和窗口大小
        javafx.scene.Scene scene = new javafx.scene.Scene(root, 800, 600);

        // 初始缩放比例
        final double[] scale = {1.0};

        // 滚轮监听
        scene.setOnScroll(event -> {
            // 获取滚轮滚动方向
            double delta = event.getDeltaY();

            // 根据滚动方向调整缩放比例
            if (delta > 0) {
                scale[0] += 0.05; // 放大
            } else {
                scale[0] -= 0.05; // 缩小
            }

            // 限制缩放比例在 0.5x ~ 1.5x 之间
            scale[0] = Math.max(0.5, Math.min(1.5, scale[0]));

            // 应用新的缩放比例
            fullImageView.setScaleX(scale[0]);
            fullImageView.setScaleY(scale[0]);
        });

        // 自动让图片适配窗口大小，但保持比例
        root.widthProperty().addListener((obs, oldVal, newVal) -> {
            fullImageView.setFitWidth(newVal.doubleValue());
        });
        root.heightProperty().addListener((obs, oldVal, newVal) -> {
            fullImageView.setFitHeight(newVal.doubleValue());
        });


        scene.setOnKeyPressed(event -> {
            switch (event.getCode()) {
                case ESCAPE:
                    stage.close();
                    break;
                default:
                    break;
            }
        });
        // 设置窗口场景
        stage.setScene(scene);
        // 显示窗口
        stage.show();
    }



}
