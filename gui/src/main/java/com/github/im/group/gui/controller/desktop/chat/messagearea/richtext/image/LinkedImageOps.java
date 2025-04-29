package com.github.im.group.gui.controller.desktop.chat.messagearea.richtext.image;

import com.github.im.group.gui.controller.desktop.chat.messagearea.richtext.FileResource;
import org.fxmisc.richtext.model.NodeSegmentOpsBase;


public class LinkedImageOps<S> extends NodeSegmentOpsBase<FileResource, S> {

    public LinkedImageOps() {
        super(new EmptyFileResource());
    }

    @Override
    public int length(FileResource fileResource) {
        return fileResource.isReal() ? 1 : 0;
    }

}
