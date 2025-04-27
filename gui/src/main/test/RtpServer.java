//import io.netty.bootstrap.ServerBootstrap;
//import io.netty.channel.*;
//import io.netty.channel.nio.NioEventLoopGroup;
//import io.netty.channel.socket.nio.NioDatagramChannel;
//import io.netty.handler.codec.rtsp.RtspDecoder;
//
//public class RtpServer {
//
//    private static final int PORT = 5004;  // RTP 端口
//
//    public static void main(String[] args) {
//        EventLoopGroup group = new NioEventLoopGroup();
//
//        try {
//            ServerBootstrap bootstrap = new ServerBootstrap();
//            bootstrap.group(group)
//                    .channel(NioDatagramChannel.class)  // UDP 传输
//                    .option(ChannelOption.SO_BROADCAST, true) // 开启广播
//                    .handler(new RtpServerInitializer());
//
//            // 启动并绑定端口
//            ChannelFuture future = bootstrap.bind(PORT).sync();
//            System.out.println("RTP 服务器已启动，监听端口：" + PORT);
//
//            // 等待服务器关闭
//            future.channel().closeFuture().sync();
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        } finally {
//            group.shutdownGracefully();
//        }
//    }
//}
//
//class RtpServerInitializer extends ChannelInitializer<Channel> {
//    @Override
//    protected void initChannel(Channel ch) throws Exception {
//        ChannelPipeline pipeline = ch.pipeline();
////        pipeline.addLast(new RtpHandler());  // 使用 Netty RTP 处理器
//        pipeline.addLast(new RtspDecoder());  // 使用 Netty RTP 处理器
//    }
//}
