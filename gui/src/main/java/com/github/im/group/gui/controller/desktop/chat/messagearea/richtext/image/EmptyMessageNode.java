package com.github.im.group.gui.controller.desktop.chat.messagearea.richtext.image;

import com.github.im.common.connect.model.proto.Chat;
import com.github.im.group.gui.controller.desktop.chat.messagearea.richtext.MessageNode;
import javafx.scene.Node;

public class EmptyMessageNode implements MessageNode {


    @Override
    public String getFilePath() {
        return "";
    }

    @Override
    public Node createNode() {
        throw new AssertionError("Unreachable code");
    }


    @Override
    public String getDescription() {
        return null;
    }

    @Override
    public Chat.MessageType getType() {
        return MessageNode.super.getType();
    }
}
