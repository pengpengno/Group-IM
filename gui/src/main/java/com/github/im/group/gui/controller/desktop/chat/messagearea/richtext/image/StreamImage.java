package com.github.im.group.gui.controller.desktop.chat.messagearea.richtext.image;

import javafx.scene.Node;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;


/**
 * A custom object which contains image file stream .
 * When rendered in the rich text editor, the image is loaded from the dataInputStream
 */
public class StreamImage implements LinkedImage {

    private final Image image;

    private byte[] imageData;

    public StreamImage(Image image) {
        this.image = image;
    }


    public Image getImage() {
        return image ;
    }

    @Override
    public boolean isReal() {
        return true;
    }

    @Override
    public String getImagePath() {
        return "";
    }

    @Override
    public String toString() {
        return String.format("StreamImage[path=%s]", "");
    }

    @Override
    public Node createNode() {
        ImageView imageView = new ImageView(image);
        imageView.setFitWidth(100);
        imageView.setPreserveRatio(true);

        imageView.setOnMouseClicked(event-> {
            // 双击放大查看
        });

        return imageView;
    }
}
