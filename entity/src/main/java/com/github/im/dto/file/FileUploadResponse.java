package com.github.im.dto.file;

import com.github.im.dto.message.FileMeta;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FileUploadResponse {
    // 必须返回
    private UUID id;
//    private String fileName;
//    private String path;
    // 文件上传后 必须返回  不可为空
    // 在上传前获取文件 id 的时候 不会返回信息
    private FileMeta fileMeta;

    /**
     * 文件的状态
     *
     */
    private String fileStatus;
}
