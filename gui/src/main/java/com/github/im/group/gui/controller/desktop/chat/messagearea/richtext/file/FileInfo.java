package com.github.im.group.gui.controller.desktop.chat.messagearea.richtext.file;

import org.springframework.core.io.Resource;

import java.util.ResourceBundle;

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


    /**
     * 文件名称
     * @return
     */
    public String getName();


    /**
     * 返回文件资源
     * @return
     */
    public Resource getFileResource();


    /**
     * <li>{@link LocalFileInfo} 返回本地文件绝对路径</li>
     * <li>{@link RemoteFileInfo} 返回文件的唯一标识即可</li>
     * @return 文件路径
     */
    public String getPath();

    /**
     * 是否下载到了本地
     * @return true:下载到本地
     */
    default public boolean isDownLoadLocal(){
        return false;
    }

    /**
     * 文件大小
     * @return 文件大小
     */
    public long getSize();


}