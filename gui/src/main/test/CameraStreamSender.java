import io.github.palexdev.mfxcore.utils.fx.SwingFXUtils;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.bytedeco.ffmpeg.global.avcodec;
import org.bytedeco.javacv.*;
import java.awt.image.BufferedImage;

public class CameraStreamSender extends Application {

    private volatile boolean running = true;
    private OpenCVFrameGrabber grabber;
    private FFmpegFrameRecorder recorder;

    // 服务器的RTP地址，暂时假设推送到本机
    private static final String RTP_URL = "udp://127.0.0.1:5004";

    @Override
    public void start(Stage primaryStage) {
        ImageView imageView = new ImageView();
        imageView.setFitWidth(640);
        imageView.setFitHeight(480);

        StackPane root = new StackPane(imageView);
        Scene scene = new Scene(root, 640, 480);

        primaryStage.setTitle("JavaFX Camera RTP Sender");
        primaryStage.setScene(scene);
        primaryStage.show();

        startCameraAndStream(imageView);

        primaryStage.setOnCloseRequest(event -> {
            running = false;
            try {
                if (grabber != null) {
                    grabber.stop();
                    grabber.release();
                }
                if (recorder != null) {
                    recorder.stop();
                    recorder.release();
                }
            } catch (FrameGrabber.Exception | FrameRecorder.Exception e) {
                e.printStackTrace();
            }
        });
    }

    private void startCameraAndStream(ImageView imageView) {
        new Thread(() -> {
            try {
                grabber = new OpenCVFrameGrabber(0);
                grabber.start();

                // 初始化 RTP Recorder
                recorder = new FFmpegFrameRecorder(RTP_URL, 640, 480);
//                recorder.setFormat("rtp");
//                recorder.setVideoCodec(avcodec.AV_CODEC_ID_H264); // 使用 H.264 编码
//                recorder.setFrameRate(30);
//                recorder.setVideoBitrate(2000000); // 2 Mbps
//                recorder.start();

                recorder.setFormat("mpegts");
                recorder.setVideoCodec(avcodec.AV_CODEC_ID_H264);
                recorder.setFrameRate(30);
                recorder.setVideoBitrate(2000000);
                recorder.start();
                Java2DFrameConverter converter = new Java2DFrameConverter();

                while (running) {
                    Frame frame = grabber.grab();
                    if (frame != null) {
                        // 显示到界面
                        BufferedImage bufferedImage = converter.getBufferedImage(frame);
                        if (bufferedImage != null) {
                            WritableImage writableImage = SwingFXUtils.toFXImage(bufferedImage, null);
                            Platform.runLater(() -> imageView.setImage(writableImage));
                        }

                        // 发送到 RTP
                        System.out.println("推流发送一帧...");
                        recorder.record(frame);

//                        recorder.record(frame);
                    }
                    Thread.sleep(33); // 30fps
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
