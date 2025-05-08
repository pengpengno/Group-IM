package com.github.im.group.gui.controller.desktop.chat.messagearea;

import com.fasterxml.jackson.databind.node.TextNode;
import com.github.im.common.connect.model.proto.Chat;
import com.github.im.dto.session.FileMeta;
import com.github.im.dto.session.MessageDTO;
import com.github.im.enums.MessageType;
import com.github.im.group.gui.controller.desktop.MessageWrapper;
import com.github.im.group.gui.controller.desktop.chat.messagearea.richtext.MessageNode;
import com.github.im.group.gui.controller.desktop.chat.messagearea.richtext.file.FileNode;
import com.github.im.group.gui.controller.desktop.chat.messagearea.richtext.file.RemoteFileInfo;
import com.github.im.group.gui.controller.desktop.chat.messagearea.richtext.file.RemoteFileService;
import com.github.im.group.gui.controller.desktop.chat.messagearea.richtext.image.StreamImage;
import com.github.im.group.gui.util.ImageUtil;
import com.github.im.group.gui.util.PathFileUtil;
import com.google.protobuf.Message;
import javafx.scene.Node;
import javafx.scene.image.Image;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.Optional;

/**
 * Description:
 * <p>
 * </p>
 *
 * @author pengpeng
 * @version 1.0
 * @since 2025/4/30
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MessageNodeService {

    private final RemoteFileService remoteFileService;

    private final ApplicationContext applicationContext;



    /**
     * 创建 消息节点 ，对于文本类型得直接返回 {@code Optional.empty() }
     * <ul>
     *     <li>{@link MessageType#TEXT } 直接返回</li>
     *     <li>{@link MessageType#FILE } 文件类型构建</li>
     * </ul>
     * @param messageWrapper 消息包装器
     * @return 消息节点
     */
    public Mono<MessageNode> createMessageNode(final MessageWrapper messageWrapper) {
        if (messageWrapper.getMessageType() != MessageType.FILE) {
            return Mono.empty();
        }
        // 如果是本地上传的文件  ， 那么直接复用 已有的messageNode

        var messageNodeOpt = messageWrapper.getMessageNode();
        return messageNodeOpt.map(Mono::just)  // 如果是
                .orElseGet(() -> remoteFileService.initFileInfo(messageWrapper)
                .flatMap(remoteFileInfo -> {
                    FileMeta fileMeta = remoteFileInfo.getFileMeta();
                    var contentType = PathFileUtil.getMessageType(fileMeta.getFilename());
                    log.debug("文件类型 {}", contentType);
                    var fileNode = new FileNode(remoteFileInfo);
                    var bean = applicationContext.getBean(FileNode.class, remoteFileInfo);

                    if (contentType == Chat.MessageType.IMAGE) {
                        return remoteFileService.download(remoteFileInfo)
                                .map(resource -> {
                                    try {
                                        Image image = ImageUtil.bytesToImage(resource.getContentAsByteArray());
                                        return new StreamImage(image);
                                    } catch (IOException e) {
                                        log.error("图片转换失败", e);
//                                        return  new FileNode(remoteFileInfo);
                                        return bean;
                                    }
                                });
                    }

                    return Mono.just(bean);

                }));

    }


}