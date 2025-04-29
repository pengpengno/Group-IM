package com.github.im.group.gui.controller.desktop.chat.messagearea.richtext.image;

import com.github.im.group.gui.controller.desktop.chat.messagearea.richtext.FileResource;
import javafx.scene.Node;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.io.File;


/**
 * A custom object which contains a file path to an image file.
 * When rendered in the rich text editor, the image is loaded from the
 * specified file.
 */
public class RealFileResource implements FileResource {

    private final String imagePath;

    private Image image;

    /**
     * Creates a new linked image object.
     *
     * @param imagePath The path to the image file.
     */
    public RealFileResource(String imagePath) {

        // if the image is below the current working directory,
        // then store as relative path name.
        String currentDir = System.getProperty("user.dir") + File.separatorChar;
        if (imagePath.startsWith(currentDir)) {
            imagePath = imagePath.substring(currentDir.length());
        }

        this.imagePath = imagePath;
    }

    @Override
    public Image getImage() {
        return image;
    }

    @Override
    public boolean isReal() {
        return true;
    }

    @Override
    public String getFilePath() {
        return imagePath;
    }

    @Override
    public String toString() {
        return String.format("RealLinkedImage[path=%s]", imagePath);
    }

    @Override
    public Node createNode() {
        Image image = new Image("file:" + imagePath); // XXX: No need to create new Image objects each time -
        this.image =       image ;                       // could be cached in the model layer
        return new ImageView(image);
    }
}
