package com.github.im.group.gui.controller.desktop.chat.messagearea.richtext.file;

import com.github.im.group.gui.controller.desktop.chat.messagearea.richtext.FileResource;
import org.springframework.core.io.Resource;

/**
 * Description: 用户封装文件信息
 * <ul>
 *     <li>本地文件 {@link LocalFileInfo}</li>
 *     <li>上传到服务端后的文件 {@link RemoteFileInfo}</li>
 * </ul>
 * <p>
 * </p>
 *
 * @author pengpeng
 * @version 1.0
 * @since 2025/4/29
 */
public interface FileInfo  {


    public String getName();


    public Resource getFileResource();


    /**
     * <li>{@link LocalFileInfo} 返回本地文件绝对路径</li>
     * <li>{@link RemoteFileInfo} 返回文件的唯一标识即可</li>
     * @return 文件路径
     */
    public String getPath();

    public long getSize();



}