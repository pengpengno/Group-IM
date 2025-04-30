package com.github.im.group.gui.controller.desktop.chat.messagearea.richtext.file;

import com.github.im.dto.session.FileMeta;
import com.github.im.group.gui.controller.desktop.chat.messagearea.richtext.file.FileInfo;
import lombok.Getter;
import org.springframework.core.io.Resource;
import reactor.core.publisher.Mono;

public class RemoteFileInfo implements FileInfo {
    @Getter
    private final FileMeta fileMeta;
    private final String fileId;
    private Resource resource;


    private RemoteFileInfo() {
        this.fileMeta = null;
        this.fileId = null;
    }

    public RemoteFileInfo(FileMeta fileMeta, String fileId) {
        this.fileMeta = fileMeta;
        this.fileId = fileId;
    }

    @Override
    public String getName() {
        return fileMeta.getFilename();
    }

    @Override
    public long getSize() {
        return fileMeta.getFileSize();
    }

    @Override
    public String getPath() {
        return fileId;
    }

    @Override
    public Resource getFileResource() {
        return resource;
    }

    public void setDownload(Mono  downloadMono){

    }

    public void setResource(Resource resource) {
        this.resource = resource;
    }
}
