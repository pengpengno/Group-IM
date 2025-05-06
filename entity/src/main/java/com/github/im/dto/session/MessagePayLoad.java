package com.github.im.dto.session;/**
 * Description: [描述方法或类的作用和功能]
 * <p>
 * </p>
 *
 * @author [peng]
 * @since 30
 */

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * Description:
 * <p>
 *     消息的拓展体 ， 如文件类型消息中 文件 元数据 ：文件名，文件大小，文件类型等
 * </p>
 *
 * @author pengpeng
 * @version 1.0
 * @since 2025/4/30
 */
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,             // 用“名称”来表示子类类型
        include = JsonTypeInfo.As.PROPERTY,     // 将类型信息包含在 JSON 的字段中
        property = "type"                       // 字段名叫 "type"，用于标识类型
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = DefaultMessagePayLoad.class, name = "TEXT"),
        @JsonSubTypes.Type(value = FileMeta.class, name = "FILE")
})
public interface MessagePayLoad {

}
