package com.github.im.group.gui.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Description:
 * <p>
 *     文件存储表 多个文件处理
 *
 *     多个文件增加索引标识
 *     即 同一份文件可以被多个消息指向 ；
 *      所有文件都是只读的状态 如果需要修改那么需要重新编辑一份
 * </p>
 *
 * @author pengpeng
 * @version 1.0
 * @since 2025/5/9
 */

@Entity
@Table(name = "file_storage", indexes = {
        @Index(name = "idx_filename", columnList = "fileName")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FileStorageTable {

    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false)
    private Long id;

    @Column(nullable = false, length = 255)
    private String fileName;

    @Column(length = 100)
    private String contentType;

    @Column(nullable = false)
    private Long size;

    private String path ;

    // 可以加字段：md5, sha256, localPath, downloadUrl 等
}