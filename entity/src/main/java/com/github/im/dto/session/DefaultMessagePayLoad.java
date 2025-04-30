package com.github.im.dto.session;

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



    public DefaultMessagePayLoad(String content) {
        this.content = content;
    }

}