package com.github.im.group.gui.util;

import javafx.scene.image.Image;
import javafx.scene.input.Clipboard;

public class ClipboardImageUtils {

    public static Image getImageFromClipboard() {
        Clipboard clipboard = Clipboard.getSystemClipboard();
        if (clipboard.hasImage()) {
            return clipboard.getImage();
        }
        return null;
    }
}
