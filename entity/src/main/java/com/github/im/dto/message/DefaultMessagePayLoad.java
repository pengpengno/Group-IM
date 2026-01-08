package com.github.im.dto.message;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * Description: 默认的消息拓展体
 * <p>
 * </p>
 *
 * @author pengpeng
 * @version 1.0
 * @since 2025/4/30
 */
@Data
public class DefaultMessagePayLoad implements MessagePayLoad{


    private final String content;


    @JsonCreator
    public DefaultMessagePayLoad(@JsonProperty("content") String content) {
        this.content = content;
    }

}