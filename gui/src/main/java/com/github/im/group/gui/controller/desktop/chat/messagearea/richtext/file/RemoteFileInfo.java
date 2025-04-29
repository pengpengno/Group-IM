package com.github.im.group.gui.controller.desktop.chat.messagearea.richtext.file;

import cn.hutool.core.io.file.FileNameUtil;
import org.springframework.core.io.Resource;

import java.nio.file.Path;

/**
 * Description:
 * <p>
 * </p>
 *
 * @author pengpeng
 * @version 1.0
 * @since 2025/4/29
 */
public class RemoteFileInfo implements FileInfo{


    @Override
    public String getName() {
        return FileNameUtil.getName("");
    }

    @Override
    public Resource getFileResource() {
        return null;
    }

    @Override
    public String getPath() {
        return null;
    }

    @Override
    public long getSize() {
        return 0;
    }
}