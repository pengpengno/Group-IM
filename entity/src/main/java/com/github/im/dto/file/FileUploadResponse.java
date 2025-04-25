package com.github.im.dto.file;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FileUploadResponse {
    private UUID id;
    private String fileName;
    private String path;
}
