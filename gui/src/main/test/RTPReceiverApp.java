import io.github.palexdev.mfxcore.utils.fx.SwingFXUtils;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Java2DFrameConverter;

import java.awt.image.BufferedImage;

public class RTPReceiverApp extends Application {
    private volatile boolean running = true;

    @Override
    public void start(Stage primaryStage) {
        ImageView imageView = new ImageView();
        imageView.setFitWidth(640);
        imageView.setFitHeight(480);

        StackPane root = new StackPane(imageView);
        Scene scene = new Scene(root, 640, 480);

        primaryStage.setTitle("RTP Receiver");
        primaryStage.setScene(scene);
        primaryStage.show();

        receiveAndDisplay(imageView);

        primaryStage.setOnCloseRequest(event -> running = false);
    }

    private void receiveAndDisplay(ImageView imageView) {
        new Thread(() -> {
            FFmpegFrameGrabber grabber = null;
            try {
//                grabber = new FFmpegFrameGrabber("rtp://127.0.0.1:5004");
//                grabber.setFormat("h264");
//                grabber.setOption("protocol_whitelist", "file,udp,rtp");
//                grabber.setOption("rtpflags", "receive_ts=1");
                grabber = new FFmpegFrameGrabber("udp://127.0.0.1:5004");
                grabber.setFormat("mpegts");
                grabber.setOption("protocol_whitelist", "file,udp,rtp"); // 保持
                grabber.start();


                grabber.start();

                Java2DFrameConverter converter = new Java2DFrameConverter();

                while (running) {
                    Frame frame = grabber.grabImage();
                    if (frame != null) {
                        BufferedImage bufferedImage = converter.convert(frame);
                        if (bufferedImage != null) {
                            WritableImage writableImage = SwingFXUtils.toFXImage(bufferedImage, null);
                            Platform.runLater(() -> imageView.setImage(writableImage));
                        }
                    }
                }

            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try {
                    if (grabber != null) {
                        grabber.stop();
                        grabber.release();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }, "RTP-Receiver-Thread").start();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
