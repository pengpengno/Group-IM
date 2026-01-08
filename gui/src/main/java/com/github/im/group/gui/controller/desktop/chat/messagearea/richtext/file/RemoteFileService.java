package com.github.im.group.gui.controller.desktop.chat.messagearea.richtext.file;

import com.github.im.dto.message.FileMeta;
import com.github.im.group.gui.api.MessageEndpoint;
import com.github.im.group.gui.controller.desktop.MessageWrapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyExtractors;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.ByteArrayOutputStream;
import java.util.Optional;
import java.util.UUID;

@Component
@Slf4j
public class RemoteFileService {

    @Resource
    private MessageEndpoint messageEndpoint;

    @Resource
    private WebClient webClient;

    /**
     * 根据 MessageWrapper 初始化 RemoteFileInfo
     * 该方法首先尝试从 MessageDTO 中直接获取 FileMeta 信息，如果成功，则直接创建并返回 RemoteFileInfo 对象
     * 如果直接获取失败，则尝试通过消息ID从远程获取完整消息内容，再次尝试获取 FileMeta 信息并创建 RemoteFileInfo 对象
     * 如果最终无法获取到 FileMeta 信息，则记录错误日志并返回空值
     *
     * @param wrapper 包含消息信息的包装类，用于初始化 RemoteFileInfo
     * @return 返回一个 Mono<RemoteFileInfo> 对象，包含初始化的 RemoteFileInfo，如果无法初始化，则返回空 Mono
     */
    public Mono<RemoteFileInfo> initFileInfo(MessageWrapper wrapper) {
        // 尝试从 MessageDTO 中直接获取 FileMeta 信息
        var dto = wrapper.getMessageDTO();
        if (dto != null && dto.getPayload() instanceof FileMeta meta) {
            return Mono.just(new RemoteFileInfo(meta, wrapper.getContent()));
        }

        // 如果直接获取失败，则尝试通过消息ID从远程获取完整消息内容
        var chatMessage = wrapper.getMessage();
        if (chatMessage != null) {
            if(chatMessage.getMsgId() != 0){

            }
            return messageEndpoint.getMessageById(chatMessage.getMsgId())
                    .map(msg -> {
                        // 再次尝试获取 FileMeta 信息并创建 RemoteFileInfo 对象
                        if (msg.getPayload() instanceof FileMeta meta) {
                            return new RemoteFileInfo(meta, wrapper.getContent());
                        }
                        // 如果最终无法获取到 FileMeta 信息，则记录错误日志
                        log.error("消息 {} 的文件信息为空", chatMessage.getMsgId());
                        return null;
                    });
        }

        // 如果所有尝试都失败，则返回空值
        return Mono.empty();
    }



    /**
     * 下载文件并设置 Resource
     */
    public Mono<org.springframework.core.io.Resource> download(final RemoteFileInfo fileInfo) {
        UUID id = UUID.fromString(fileInfo.getPath());

        return webClient.get()
                .uri("/api/files/download/{fileId}", id)
                .accept(MediaType.APPLICATION_OCTET_STREAM)
                .exchangeToMono(response -> {
                    var body = response.body(BodyExtractors.toDataBuffers());
                    HttpHeaders headers = response.headers().asHttpHeaders();
                    String filename = Optional.of(headers.getContentDisposition())
//                            .map(ContentDisposition::getFilename)
                            .map(ContentDisposition::getFilename)
                            .orElse(id.toString());

                    return DataBufferUtils.join(response.bodyToFlux(DataBuffer.class))
                            .flatMap(buffer -> {
                                try {
                                    // 限制最大文件大小 100MB
                                    int maxSize = 100 * 1024 * 1024;
                                    if (buffer.readableByteCount() > maxSize) {
                                        DataBufferUtils.release(buffer);
                                        return Mono.error(new IllegalStateException("文件过大，超过限制"));
                                    }

                                    byte[] data = new byte[buffer.readableByteCount()];
                                    buffer.read(data);
                                    return Mono.just(new ByteArrayResource(data));
                                } finally {
                                    DataBufferUtils.release(buffer); // 避免内存泄漏
                                    log.info("下载完成: {}", filename);
                                }
                            });
//                    return DataBufferUtils.write(body, outputStream)
//                            .doOnTerminate(() -> {
//                                log.info("下载完成: {}", filename);
//
//                            })
//                            .subscribeOn(Schedulers.boundedElastic())
//                            .then(Mono.just(new ByteArrayResource(outputStream.toByteArray())));
                });
    }

    public Mono<Void> downloadToMemory(RemoteFileInfo fileInfo) {
        UUID id = UUID.fromString(fileInfo.getPath());
        var outputStream = new ByteArrayOutputStream();

        return webClient.get()
                .uri("/api/files/download/{fileId}", id)
                .accept(MediaType.APPLICATION_OCTET_STREAM)
                .exchangeToMono(response -> {
                    var body = response.body(BodyExtractors.toDataBuffers());
                    HttpHeaders headers = response.headers().asHttpHeaders();
                    String filename = Optional.of(headers.getContentDisposition())
//                            .map(ContentDisposition::getFilename)
                            .map(ContentDisposition::getFilename)
                            .orElse(id.toString());

                    return DataBufferUtils.write(body, outputStream)
                            .doOnTerminate(() -> {
                                log.info("下载完成: {}", filename);
                                fileInfo.setResource(new ByteArrayResource(outputStream.toByteArray()));
                            })
                            .subscribeOn(Schedulers.boundedElastic())
                            .then();
                });
    }
}
