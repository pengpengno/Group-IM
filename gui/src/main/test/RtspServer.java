//import io.netty.bootstrap.ServerBootstrap;
//import io.netty.channel.*;
//import io.netty.channel.nio.NioEventLoopGroup;
//import io.netty.channel.socket.nio.NioServerSocketChannel;
//import io.netty.handler.codec.rtsp.RtspDecoder;
//import io.netty.handler.codec.rtsp.RtspEncoder;
//import io.netty.handler.codec.rtsp.RtspMethods;
//
//public class RtspServer {
//
//    private static final int PORT = 554;  // RTSP 服务器端口
//
//    public static void main(String[] args) {
//        EventLoopGroup bossGroup = new NioEventLoopGroup();
//        EventLoopGroup workerGroup = new NioEventLoopGroup();
//
//        try {
//            ServerBootstrap bootstrap = new ServerBootstrap();
//            bootstrap.group(bossGroup, workerGroup)
//                    .channel(NioServerSocketChannel.class)
//                    .childHandler(new RtspServerInitializer());
//
//            // 启动服务器并绑定端口
//            bootstrap.bind(PORT).sync().channel().closeFuture().sync();
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        } finally {
//            bossGroup.shutdownGracefully();
//            workerGroup.shutdownGracefully();
//        }
//    }
//
//    static class RtspServerInitializer extends ChannelInitializer<Channel> {
//        @Override
//        protected void initChannel(Channel ch) throws Exception {
//            ChannelPipeline pipeline = ch.pipeline();
//
//            // 使用 RtspDecoder 解码 RTSP 请求
//            pipeline.addLast(new RtspDecoder());
//
//            // 使用 RtspEncoder 编码 RTSP 响应
//            pipeline.addLast(new RtspEncoder());
//
//            // 处理 RTSP 请求的具体业务逻辑
//            pipeline.addLast(new RtspServerHandler());
//        }
//    }
//
//    static class RtspServerHandler extends SimpleChannelInboundHandler<RtspMethods> {
//        @Override
//        protected void channelRead0(ChannelHandlerContext ctx, RtspRequest msg) throws Exception {
//            // 处理接收到的 RTSP 请求
//            System.out.println("接收到 RTSP 请求: " + msg);
//
//            // 创建 RTSP 响应并发送
//            RtspResponse response = new RtspResponse(RtspResponseStatus.OK);
//            ctx.writeAndFlush(response);
//        }
//    }
//}
