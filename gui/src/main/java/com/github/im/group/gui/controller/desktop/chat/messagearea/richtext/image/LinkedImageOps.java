package com.github.im.group.gui.controller.desktop.chat.messagearea.richtext.image;

import org.fxmisc.richtext.model.NodeSegmentOpsBase;


public class LinkedImageOps<S> extends NodeSegmentOpsBase<LinkedImage, S> {

    public LinkedImageOps() {
        super(new EmptyLinkedImage());
    }

    @Override
    public int length(LinkedImage linkedImage) {
        return linkedImage.isReal() ? 1 : 0;
    }

}
