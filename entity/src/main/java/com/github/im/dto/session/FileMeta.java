package com.github.im.dto.session;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Description: 文件元数据
 * <p>
 * </p>
 *
 * @author pengpeng
 * @version 1.0
 * @since 2025/4/30
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileMeta implements MessagePayLoad{

    private String fileId;   // 服务端生成的文件Id  uuid

    private String clientId; // 客户端上传时生成的文件Id

    private String filename;

    private long fileSize;

    private String contentType;

    private String hash ;

    /**
     * 文件时长 单位 mills
     * 适用于 视频音频文件
     */
    private Long duration;


}