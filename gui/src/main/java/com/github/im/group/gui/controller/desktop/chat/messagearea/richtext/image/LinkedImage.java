package com.github.im.group.gui.controller.desktop.chat.messagearea.richtext.image;

import com.github.im.group.gui.util.ImageUtil;
import javafx.scene.Node;
import javafx.scene.image.Image;
import org.fxmisc.richtext.model.Codec;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public interface LinkedImage {

    static <S> Codec<Image>  imageCodec (){
     return new ImageCodec("png");
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


    static <S> Codec<LinkedImage> codec() {
        return new Codec<LinkedImage>() {
            @Override
            public String getName() {
                return "LinkedImage";
            }

            @Override
            public void encode(DataOutputStream os, LinkedImage linkedImage) throws IOException {

                if (linkedImage instanceof RealLinkedImage real) {
                    os.writeByte(1); // type 1
                    Codec.STRING_CODEC.encode(os, real.getImagePath());
                }
                else if (linkedImage instanceof StreamImage stream) {
                    os.writeByte(2); // type 2
                    imageCodec().encode(os,stream.getImage());

                }
                else {
                    os.writeBoolean(false);
                }
            }

            @Override
            public LinkedImage decode(DataInputStream is) throws IOException {
                if (is.readInt() == 1) {
                    String imagePath = Codec.STRING_CODEC.decode(is);
                    imagePath = imagePath.replace("\\",  "/");
                    return new RealLinkedImage(imagePath);
                }else if(is.readInt() == 2){

                    return new StreamImage(imageCodec().decode(is));
                }
                else {
                    return new EmptyLinkedImage();
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
    String getImagePath();

    Node createNode();
}
