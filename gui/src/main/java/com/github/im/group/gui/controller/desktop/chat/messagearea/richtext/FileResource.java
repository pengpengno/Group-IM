package com.github.im.group.gui.controller.desktop.chat.messagearea.richtext;

import com.github.im.group.gui.controller.desktop.chat.messagearea.richtext.file.FileDisplay;
import com.github.im.group.gui.controller.desktop.chat.messagearea.richtext.file.FileInfo;
import com.github.im.group.gui.controller.desktop.chat.messagearea.richtext.file.LocalFileInfo;
import com.github.im.group.gui.controller.desktop.chat.messagearea.richtext.file.RemoteFileInfo;
import com.github.im.group.gui.controller.desktop.chat.messagearea.richtext.image.EmptyFileResource;
import com.github.im.group.gui.controller.desktop.chat.messagearea.richtext.image.RealFileResource;
import com.github.im.group.gui.controller.desktop.chat.messagearea.richtext.image.StreamImage;
import com.github.im.group.gui.util.ImageUtil;
import javafx.scene.Node;
import javafx.scene.image.Image;
import org.fxmisc.richtext.model.Codec;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;

import java.io.*;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;

public interface FileResource {

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
//            os.write(file.getContentAsByteArray());
//            FileInfo

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
//                TODO  待实现
                // 这里是远程文件，需要先下载到本地，再获取到本地路径
                var pathString = remoteFileInfo.getPath();
                var length = pathString.length();
                os.writeByte(0x11); // 0x11 为 remoteFileInfo
                os.writeInt(length);
                os.write(pathString.getBytes());
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
//                TODO 待实现
                var length = is.readInt();
                var pathString = new String(is.readNBytes(length));
//                return new RemoteFileInfo(pathString);
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


    default Image getImage() {
        return null;
    }


    static <S> Codec<FileResource> codec() {
        return new Codec<FileResource>() {
            @Override
            public String getName() {
                return "LinkedImage";
            }

            @Override
            public void encode(DataOutputStream os, FileResource fileResource) throws IOException {

                if (fileResource instanceof RealFileResource real) {
                    os.writeByte(1); // type 1
                    Codec.STRING_CODEC.encode(os, real.getFilePath());
                }
                else if (fileResource instanceof StreamImage stream) {
                    os.writeByte(2); // type 2
                    imageCodec().encode(os,stream.getImage());
                }
                else if (fileResource instanceof FileDisplay fileDisplay) {
                    os.writeByte(3); // type 3
                    fileCodec().encode(os , fileDisplay.getFileInfo());
                }
                else {
                    os.writeBoolean(false);
                }
            }

            @Override
            public FileResource decode(DataInputStream is) throws IOException {
                if (is.readInt() == 1) {
                    String imagePath = Codec.STRING_CODEC.decode(is);
                    imagePath = imagePath.replace("\\",  "/");
                    return new RealFileResource(imagePath);
                }else if(is.readInt() == 2){
                    // stream 的图片信息
                    return new StreamImage(imageCodec().decode(is));
                }
                else if(is.readInt() == 3){
                    // 文件信息
                    return new FileDisplay(fileCodec().decode(is));
                }
                else {
                    return new EmptyFileResource();
                }
            }
        };
    }

    default public byte[] getBytes(){
        return new byte[0];
    }

    boolean isReal();

    /**
     * @return The path of the image to render.
     */
    String getFilePath();

    Node createNode();
}
