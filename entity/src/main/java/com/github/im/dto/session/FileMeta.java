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


    private String filename;

    private long fileSize;

    private String contentType;

    private String hash ;


}