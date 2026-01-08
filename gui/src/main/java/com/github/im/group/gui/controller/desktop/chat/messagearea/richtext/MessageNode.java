package com.github.im.group.gui.controller.desktop.chat.messagearea.richtext;

import com.github.im.common.connect.model.proto.Chat;
import com.github.im.dto.message.FileMeta;
import com.github.im.group.gui.controller.desktop.chat.messagearea.richtext.file.FileNode;
import com.github.im.group.gui.controller.desktop.chat.messagearea.richtext.file.FileInfo;
import com.github.im.group.gui.controller.desktop.chat.messagearea.richtext.file.LocalFileInfo;
import com.github.im.group.gui.controller.desktop.chat.messagearea.richtext.file.RemoteFileInfo;
import com.github.im.group.gui.controller.desktop.chat.messagearea.richtext.image.EmptyMessageNode;
import com.github.im.group.gui.controller.desktop.chat.messagearea.richtext.image.SystemPathImage;
import com.github.im.group.gui.controller.desktop.chat.messagearea.richtext.image.StreamImage;
import com.github.im.group.gui.util.ImageUtil;
import javafx.scene.Node;
import javafx.scene.image.Image;
import org.fxmisc.richtext.model.Codec;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;

/**
 * 消息展示的节点 如
 * <ul>
 *     <li>图片</li>
 *     <li>超链接</li>
 *     <li>文件</li>
 * </ul>
 */
public interface MessageNode {


    public String getDescription();

    /**
     * 图像 文件的编码
     * @return
     * @param <S>
     */
    static <S> Codec<Image>  imageCodec (){
         return new ImageCodec("png");
    }

    static <S> Codec<FileInfo>  fileCodec (){
        return new FileCodec();
    }

    static class FileCodec implements Codec<FileInfo> {

        @Override
        public String getName() {
            return "fileCodec";
        }

        @Override
        public void encode(DataOutputStream os, FileInfo file) throws IOException {

            /**
             * 本地文件 和远程文件的  encode 方式不同
             *  1. 先写入属性定长
             *  2. 写入属性
             */

            if(file instanceof LocalFileInfo localFileInfo){
                // 这里是 本地文件直接给到 Path 路径即可
                var pathString = localFileInfo.getPath();
                var length = pathString.length();
                os.writeByte(0x10); // 0x10 为 localFileInfo
                os.writeInt(length);
                os.write(pathString.getBytes());
            }else if(file instanceof RemoteFileInfo remoteFileInfo){
                // 这里是远程文件，需要先下载到本地，再获取到本地路径
                var fileId =  remoteFileInfo.getPath();
                var length = fileId.length();
                os.writeByte(0x11); // 0x11 为 remoteFileInfo
                os.writeInt(length);
                os.writeBytes(fileId);
                var fileMeta = remoteFileInfo.getFileMeta();


                // 写入 fileMeta.filename
                var nameBytes = fileMeta.getFilename().getBytes(StandardCharsets.UTF_8);
                os.writeInt(nameBytes.length);
                os.write(nameBytes);

                // 写入 fileMeta.fileSize
                os.writeLong(fileMeta.getFileSize());

                // 写入 fileMeta.mimeType
                var mimeBytes = fileMeta.getContentType().getBytes(StandardCharsets.UTF_8);
                os.writeInt(mimeBytes.length);
                os.write(mimeBytes);

            }
        }
        @Override
        public FileInfo decode(DataInputStream is) throws IOException {
            var type = is.readByte();
            if (type == 0x10){
                var length = is.readInt();
                var pathString = new String(is.readNBytes(length));
                return new LocalFileInfo(Paths.get(pathString));
            }
            else if (type == 0x11){

                // 读取 fileId
                var fileIdLength = is.readInt();
                var fileId = new String(is.readNBytes(fileIdLength), StandardCharsets.UTF_8);

                // 读取 fileMeta.filename
                var nameLength = is.readInt();
                var filename = new String(is.readNBytes(nameLength), StandardCharsets.UTF_8);

                // 读取 fileMeta.fileSize
                var fileSize = is.readLong();

                // 读取 fileMeta.mimeType
                var mimeLength = is.readInt();
                var mimeType = new String(is.readNBytes(mimeLength), StandardCharsets.UTF_8);

                // 组装 FileMeta 和 RemoteFileInfo
                var fileMeta = new FileMeta();
                fileMeta.setFilename(filename);
                fileMeta.setFileSize(fileSize);
                fileMeta.setContentType(mimeType);

                var remote = new RemoteFileInfo(fileMeta,fileId);  // 自定义构造函数
//                remote.setFileMeta(fileMeta);            // 提供 setter
                return remote;
            }
            return null;
        }
    }


    static class ImageCodec implements Codec<Image> {

        private final String format ;

        public ImageCodec(String format) {
            if(format == null){
                format = "png";
            }
            this.format = format;

        }

        @Override
        public String getName() {
            return "ImageCodec:" + format;
        }

        @Override
        public void encode(DataOutputStream os, Image image) throws IOException {
            byte[] bytes = ImageUtil.imageToBytes(image,format);
            os.writeInt(bytes.length);
            os.write(bytes);
        }

        @Override
        public Image decode(DataInputStream is) throws IOException {
            int length = is.readInt();
            byte[] bytes = is.readNBytes(length);
            return new Image(new ByteArrayInputStream(bytes));
        }
    }



    static <S> Codec<MessageNode> codec() {
        return new Codec<MessageNode>() {
            @Override
            public String getName() {
                return "LinkedImage";
            }

            @Override
            public void encode(DataOutputStream os, MessageNode messageNode) throws IOException {

                if (messageNode instanceof SystemPathImage real) {
                    os.writeByte(1); // type 1
                    Codec.STRING_CODEC.encode(os, real.getFilePath());
                }
                else if (messageNode instanceof StreamImage stream) {
                    os.writeByte(2); // type 2
//                    imageCodec().encode(os,stream.getImage());
                    imageCodec().encode(os,stream.getImage());
                }
                else if (messageNode instanceof FileNode fileNode) {
                    os.writeByte(3); // type 3
                    fileCodec().encode(os , fileNode.getFileInfo());
                }
                else {
                    os.writeBoolean(false);
                }
            }

            @Override
            public MessageNode decode(DataInputStream is) throws IOException {
                if (is.readInt() == 1) {
                    String imagePath = Codec.STRING_CODEC.decode(is);
                    imagePath = imagePath.replace("\\",  "/");
                    return new SystemPathImage(imagePath);
                }else if(is.readInt() == 2){
                    // stream 的图片信息
                    return new StreamImage(imageCodec().decode(is));
                }
                else if(is.readInt() == 3){
                    // 文件信息
                    return new FileNode(fileCodec().decode(is));
                }
                else {
                    return new EmptyMessageNode();
                }
            }
        };
    }

    default public byte[] getBytes(){
        return new byte[0];
    }


    default Chat.MessageType getType() {
        return Chat.MessageType.FILE;
    }

    /**
     * 返回文件的绝对路径
     * @return The path of the image to render.
     */
    default String getFilePath(){
        return null;
    };

    Node createNode();
}
