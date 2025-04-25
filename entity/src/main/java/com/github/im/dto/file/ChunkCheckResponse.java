package com.github.im.dto.file;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChunkCheckResponse {

    private String fileHash;

    /**
     * 已上传的 chunk 序号列表（从 0 开始）
     */
    private List<Integer> uploadedChunks;
}
