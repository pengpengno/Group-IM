package com.github.im.server.workbench.common.integration;

import com.github.im.server.model.FileResource;
import com.github.im.server.model.enums.FileStatus;
import com.github.im.server.repository.FileResourceRepository;
import com.github.im.server.workbench.common.error.WorkbenchErrorCode;
import com.github.im.server.workbench.common.error.WorkbenchException;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class RepositoryFileAdapter implements FileAdapter {

    private final FileResourceRepository fileResourceRepository;

    public RepositoryFileAdapter(FileResourceRepository fileResourceRepository) {
        this.fileResourceRepository = fileResourceRepository;
    }

    @Override
    public boolean isAvailable(UUID fileId) {
        if (fileId == null) {
            return false;
        }
        return fileResourceRepository.findById(fileId)
                .map(file -> file.getStatus() == FileStatus.NORMAL)
                .orElse(false);
    }

    @Override
    public WorkbenchFileRef requireAvailable(UUID fileId) {
        FileResource file = fileResourceRepository.findById(fileId)
                .filter(resource -> resource.getStatus() == FileStatus.NORMAL)
                .orElseThrow(() -> WorkbenchException.notFound(
                        WorkbenchErrorCode.FILE_NOT_AVAILABLE,
                        "附件不存在或当前不可用"
                ));
        return new WorkbenchFileRef(file.getId(), file.getOriginalName(), file.getContentType(), file.getSize());
    }
}
