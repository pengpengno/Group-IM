package com.github.im.group.gui.controller.desktop.chat.messagearea.richtext.image;

import com.github.im.group.gui.controller.desktop.chat.messagearea.richtext.MessageNode;
import com.github.im.group.gui.controller.desktop.chat.messagearea.richtext.file.FileNode;
import org.fxmisc.richtext.model.NodeSegmentOpsBase;


public class LinkedImageOps<S> extends NodeSegmentOpsBase<MessageNode, S> {

    public LinkedImageOps() {
        super(new EmptyMessageNode());
    }

    @Override
    public int length(MessageNode mDefaultNode) {
        if (mDefaultNode instanceof  EmptyMessageNode){
            return 0;
        }

        return 1;
    }

}
