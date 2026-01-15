package com.github.im.server.repository;

import com.github.im.server.model.MediaFileResource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface MediaFileResourceRepository extends JpaRepository<MediaFileResource, Long> {

    /**
     * 根据文件ID查找媒体资源
     * @param fileId 文件ID
     * @return 媒体资源
     */
    MediaFileResource findByFileId(UUID fileId);
}