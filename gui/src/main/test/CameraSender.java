import org.bytedeco.javacv.*;
import org.bytedeco.ffmpeg.global.avcodec;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class CameraSender {
    public static void main(String[] args) throws Exception {
        int width = 320, height = 240;
        InetAddress targetAddress = InetAddress.getByName("127.0.0.1");
        int port = 9999;

        // 摄像头初始化
        OpenCVFrameGrabber grabber = new OpenCVFrameGrabber(0);
        grabber.setImageWidth(width);
        grabber.setImageHeight(height);
        grabber.start();

        // 视频编码器（使用 H264 编码）
        FFmpegFrameRecorder recorder = new FFmpegFrameRecorder("udp://" + targetAddress.getHostAddress() + ":" + port, width, height);
        recorder.setVideoCodec(avcodec.AV_CODEC_ID_H264);
        recorder.setFormat("mpegts");  // TS over UDP
        recorder.setFrameRate(15);
        recorder.setVideoBitrate(200000);
        recorder.start();

        while (true) {
            Frame frame = grabber.grab();
            if (frame != null) {
                recorder.record(frame);
            }
        }
    }
}
