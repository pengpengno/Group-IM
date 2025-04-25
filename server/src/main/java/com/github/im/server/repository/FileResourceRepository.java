package com.github.im.server.repository;

import com.github.im.server.model.FileResource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface FileResourceRepository extends JpaRepository<FileResource, UUID> {


}
