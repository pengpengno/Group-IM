package com.github.im.group.gui.controller.desktop.chat.messagearea.hyperlink;

import com.github.im.common.connect.model.proto.Chat;
import com.github.im.group.gui.controller.desktop.chat.messagearea.richtext.MessageNode;
import javafx.scene.Node;

public class Hyperlink implements MessageNode {

    private final String originalDisplayedText;
    private final String displayedText;
    private final String link;


    @Override
    public String getDescription() {
        return originalDisplayedText;
    }

    @Override
    public byte[] getBytes() {
        return MessageNode.super.getBytes();
    }

    @Override
    public Chat.MessageType getType() {
        return MessageNode.super.getType();
    }


    @Override
    public Node createNode() {
        return null;
    }

    Hyperlink(String originalDisplayedText, String displayedText, String link) {
        this.originalDisplayedText = originalDisplayedText;
        this.displayedText = displayedText;
        this.link = link;
    }

    public boolean isEmpty() {
        return length() == 0;
    }

    public boolean isReal() {
        return length() > 0;
    }

    public boolean shareSameAncestor(Hyperlink other) {
        return link.equals(other.link);
    }

    public int length() {
        return displayedText.length();
    }

    public char charAt(int index) {
        return isEmpty() ? '\0' : displayedText.charAt(index);
    }

    public String getOriginalDisplayedText() { return originalDisplayedText; }

    public String getDisplayedText() {
        return displayedText;
    }

    public String getLink() {
        return link;
    }

    public Hyperlink subSequence(int start, int end) {
        return new Hyperlink(originalDisplayedText, displayedText.substring(start, end), link);
    }

    public Hyperlink subSequence(int start) {
        return new Hyperlink(originalDisplayedText, displayedText.substring(start), link);
    }

    public Hyperlink mapDisplayedText(String text) {
        return new Hyperlink(originalDisplayedText, text, link);
    }

    @Override
    public String toString() {
        return isEmpty()
                ? String.format("EmptyHyperlink[original=%s link=%s]", originalDisplayedText, link)
                : String.format("RealHyperlink[original=%s displayedText=%s, link=%s]",
                                    originalDisplayedText, displayedText, link);
    }

}
