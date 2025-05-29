import org.bytedeco.javacv.*;

public class CameraReceiver {
    public static void main(String[] args) throws Exception {
        // 接收地址（UDP TS 流）
        String input = "udp://@:9999";

        FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(input);
        grabber.setFormat("mpegts");
        grabber.start();

        CanvasFrame canvas = new CanvasFrame("Remote Video", CanvasFrame.getDefaultGamma() / grabber.getGamma());
        canvas.setDefaultCloseOperation(javax.swing.JFrame.EXIT_ON_CLOSE);
        canvas.setCanvasSize(grabber.getImageWidth(), grabber.getImageHeight());

        while (canvas.isVisible()) {
            Frame frame = grabber.grab();
            if (frame != null) {
                canvas.showFrame(frame);
            }
        }

        grabber.stop();
        canvas.dispose();
    }
}
