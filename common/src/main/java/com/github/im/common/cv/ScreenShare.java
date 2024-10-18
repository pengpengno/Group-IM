//package com.github.meeting.common.cv;
//
//import io.netty.buffer.ByteBuf;
//import io.netty.buffer.PooledByteBufAllocator;
//import lombok.SneakyThrows;
//import lombok.extern.slf4j.Slf4j;
//import org.bytedeco.ffmpeg.global.avcodec;
//import org.bytedeco.javacv.*;
//import org.bytedeco.javacv.Frame;
//
//import java.awt.*;
//import java.io.ByteArrayOutputStream;
//import java.util.function.Consumer;
//
//
//@Slf4j
//public class ScreenShare {
//
//
//    private FrameGrabber frameGrabber ;
//
//private Integer refreshRate;
//
//    private FrameRecorder frameRecorder ;
//
//
//    private ByteArrayOutputStream bos = new ByteArrayOutputStream();
//
//
//    @SneakyThrows
//    public ScreenShare startGrabber() {
//
//        Integer screen = 0;
//
//        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
//
//        var screenDevices = ge.getScreenDevices();
//
//        for (var screenDevice : screenDevices) {
//            log.info("{}",screenDevice);
//        }
//
//        var screenDevice = screenDevices[screen];
//
//        refreshRate = screenDevice.getDisplayMode().getRefreshRate();
//
//        FFmpegFrameGrabber desktop = new FFmpegFrameGrabber("desktop");
//
//        desktop.setFormat("gdigrab");
//
//        desktop.setFrameRate(refreshRate);
//
//        desktop.setVideoCodec(avcodec.AV_CODEC_ID_H264);
//
//
//        frameGrabber = desktop ;
//
//        frameGrabber.start();
//
//        return this;
//
//    }
//    @SneakyThrows
//    public void recorderStart () {
//
//        var recorder = new FFmpegFrameRecorder(bos, frameGrabber.getImageWidth(), frameGrabber.getImageHeight(),
//                frameGrabber.getAudioChannels());
//
//        recorder.setFormat("avi");
//
//        recorder.setGopSize(refreshRate * 2);
//
//        recorder.setFrameRate(refreshRate);
//
//        recorder.setVideoBitrate(2000000);
//
//        frameRecorder =  recorder;
//
//        frameRecorder.start();
//
//    }
//
//    public Frame grab () throws FrameGrabber.Exception {
//        return frameGrabber.grab();
//    }
//    public void recorder (Frame frame) throws FrameRecorder.Exception {
//         frameRecorder.record(frame);
//    }
//
//    public void release () throws FrameRecorder.Exception, FrameGrabber.Exception {
//
//            frameGrabber.stop();
//
//            frameRecorder.stop();
//
//            frameGrabber.release();
//            frameRecorder.release();
//
//    }
//
//
//
//    public void configSendConsumer (Consumer<ByteBuf> consumer) {
//        new Thread(()-> {
//            while (true){
////                recorder.get
//                if (bos.size() > 0){
//                    ByteBuf buffer = PooledByteBufAllocator.DEFAULT.buffer();
//
//                    buffer.writeBytes(bos.toByteArray());
////
////                    DefaultFullHttpRequest defaultFullHttpRequest = new DefaultFullHttpRequest(RtspVersions.RTSP_1_0,
////                            RtspMethods.OPTIONS, "/live", buffer);
//                    consumer.accept(buffer);
//
//                    bos.reset();
//                }
//
//            }
//        }).start();
//    }
//
////    @SneakyThrows
////    public void start() {
////        frameGrabber.start();
////
////        frameRecorder.start();
////    }
//}
