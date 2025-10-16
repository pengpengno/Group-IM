package com.github.im.server.model;

import com.github.im.server.model.enums.FileStatus;
import com.github.im.server.model.enums.StorageType;
import jakarta.persistence.*;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

/**
 * 媒体类型的文件资源 存储
 *  存储  视频  图片 的一些额外元数据信息
 *
 *  如 ： 缩略图
 *   通过 FFEPGE 读取 媒体 第一帧 存储 ，
 *    图片资源则将其像素化， 降低其清晰图处理， 通常应该存在小图 、 大图 、 中图 等多个，
 *    TODO
 *      优先 实现中图， 分辨率 需要定义下规范
// */
//@Entity
//@Table(name = "media_file_resource")
//@Data
//    TODO  媒体资源缩略图文件的实现

public class MediaFileResource {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;



    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "file_id", referencedColumnName = "id", nullable = false, unique = true)
    private FileResource  file;

    private String thumbnail;




}
