package com.github.im.group.gui.controller.desktop.chat.messagearea.richtext.file;

import cn.hutool.core.io.file.FileNameUtil;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

import java.io.File;
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
public class LocalFileInfo implements FileInfo{


    private final Path path ;
    public File getFile() {
        return  path.toFile();
    }

    public LocalFileInfo(Path path) {
        this.path = path;
    }


    @Override
    public String getName() {
        return FileNameUtil.getName(getFile());
    }

    @Override
    public String getPath() {

        return path.toFile().getAbsolutePath();
    }


    @Override
    public long getSize() {
        return getFile().length();
    }

    @Override
    public Resource getFileResource() {

        return new FileSystemResource(getFile());
    }



}