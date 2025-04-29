package com.github.im.group.gui.controller.desktop.chat.messagearea.richtext.image;

import com.github.im.group.gui.controller.desktop.chat.messagearea.richtext.FileResource;
import javafx.scene.Node;

public class EmptyFileResource implements FileResource {

    @Override
    public boolean isReal() {
        return false;
    }

    @Override
    public String getFilePath() {
        return "";
    }

    @Override
    public Node createNode() {
        throw new AssertionError("Unreachable code");
    }
}
