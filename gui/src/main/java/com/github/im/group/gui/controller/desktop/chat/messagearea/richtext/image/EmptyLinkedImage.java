package com.github.im.group.gui.controller.desktop.chat.messagearea.richtext.image;

import com.github.im.group.gui.controller.desktop.chat.messagearea.richtext.image.LinkedImage;
import javafx.scene.Node;

public class EmptyLinkedImage implements LinkedImage {

    @Override
    public boolean isReal() {
        return false;
    }

    @Override
    public String getImagePath() {
        return "";
    }

    @Override
    public Node createNode() {
        throw new AssertionError("Unreachable code");
    }
}
