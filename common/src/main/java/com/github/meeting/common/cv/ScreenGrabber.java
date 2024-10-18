//package com.github.meeting.common.cv;
//
//import cn.hutool.core.exceptions.ExceptionUtil;
//import com.google.inject.Singleton;
//import lombok.SneakyThrows;
//import lombok.extern.slf4j.Slf4j;
//import org.bytedeco.ffmpeg.global.avcodec;
//import org.bytedeco.javacv.CanvasFrame;
//import org.bytedeco.javacv.FFmpegFrameGrabber;
//import org.bytedeco.javacv.Frame;
//import org.bytedeco.javacv.FrameGrabber;
//import org.bytedeco.javacv.OpenCVFrameGrabber;
//
//import javax.swing.*;
//import java.awt.*;
//
//@Slf4j
///*
// * 屏幕录制器
// */
//@Singleton
//public class ScreenGrabber {
//
//
//    /***
//     * 桌面屏幕录制器
//     * @return 返回 FFmpeg 屏幕录制器
//     */
//    public FrameGrabber desktopScreenCapture () {
//
////        Integer screen = 0;
////
////        FrameGrabber grabber = new FFmpegFrameGrabber("desktop");
////
////        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
////
////        var screenDevices = ge.getScreenDevices();
////
////        var screenDevice = screenDevices[screen];
////
////        var refreshRate = screenDevice.getDisplayMode().getRefreshRate();
////
////        log.info("refreshRate : {}",refreshRate);
////
////        refreshRate = 10 ;
//
//        try {
//            var desktop = FFmpegFrameGrabber.createDefault("desktop");
//
//            var audio = FFmpegFrameGrabber.createDefault("audio=virtual-auodi-capturer");
//
//            audio.setFormat("dshow");
//
//            desktop.setFormat("gdigrab");
//
//            desktop.setFrameRate(540);
//
//            desktop.setVideoCodec(avcodec.AV_CODEC_ID_H264);
//
//            desktop.setVideoFrameNumber(0);
//
//            desktop.start();
//            return desktop;
//        }catch (Exception e) {
//
//            log.error("创建本地分享异常{}", ExceptionUtil.stacktraceToString(e));
//
//        }
//        return null;
//
////        return grabber;
//    }
//
//
//    @SneakyThrows
//    public FrameGrabber camera() {
//
//        OpenCVFrameGrabber grabber = new OpenCVFrameGrabber(0);
//        grabber.start();
////        CanvasFrame canvasFrame = new CanvasFrame("Camera Preview",
////                CanvasFrame.getDefaultGamma() / grabber.getGamma());
////        canvasFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
////        while (true) {
////            Frame frame = grabber.grab();
//////            IplImage frame = (IplImage) grab;
////            if (frame == null) {
////                break;
////            }
////            canvasFrame.showImage(frame);
//////            opencv_imgproc.cvResize(frame, frame, Size.ZERO, 0.5, 0.5, opencv_imgproc.CV_INTER_LINEAR);
////        }
////        canvasFrame.dispose();
////        grabber.stop();
//        return grabber;
////        try {
////            OpenCVFrameGrabber openCVFrameGrabber = new OpenCVFrameGrabber(0);
////            openCVFrameGrabber.start();
////
////        }
////        return null;
//
//    }
//
//    public FrameGrabber desktop() {
//        try {
//            var desktop = FFmpegFrameGrabber.createDefault("desktop");
//
//            var audio = FFmpegFrameGrabber.createDefault("audio=virtual-auodi-capturer");
//
//            audio.setFormat("dshow");
//
//            desktop.setFormat("gdigrab");
//
//            desktop.setFrameRate(540);
//
//            desktop.setVideoCodec(avcodec.AV_CODEC_ID_H264);
//
//            desktop.setVideoFrameNumber(0);
//
//            desktop.start();
//            return desktop;
//        }catch (Exception e) {
//
//            log.error("创建本地分享异常{}", ExceptionUtil.stacktraceToString(e));
//
//        }
//        return null;
//    }
//
//
//
//}
