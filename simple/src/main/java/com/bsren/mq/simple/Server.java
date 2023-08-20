package com.bsren.mq.simple;

import com.bsren.mq.simple.message.Message;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;

import java.net.UnknownHostException;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class Server {

    Map<Integer, Message> msgs = new ConcurrentHashMap<>();

    AtomicInteger atomicInteger = new AtomicInteger();


    public static final int PORT =8080;

    public static void main(String[] args) throws UnknownHostException, InterruptedException {
        Server server = new Server();
        server.start();
    }



    public void start() throws UnknownHostException, InterruptedException {
        EventLoopGroup eventLoopGroup = new NioEventLoopGroup();
        ServerBootstrap bootstrap = new ServerBootstrap();
        bootstrap.group(eventLoopGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<NioSocketChannel>() {
                    @Override
                    protected void initChannel(NioSocketChannel ch) throws Exception {
                        ChannelPipeline pipeline = ch.pipeline();
                        pipeline.addLast(new NettyEncoder());
                        pipeline.addLast(new NettyDecoder());
                        pipeline.addLast(new NettyHandler());
                    }
                }).bind(PORT);
    }

    class NettyHandler extends ChannelInboundHandlerAdapter {
        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            if(msg instanceof RemotingCommand){
                int i = atomicInteger.incrementAndGet();
                int code = ((RemotingCommand) msg).getCode();
                switch (code){
                    case RequestCode.SEND_MESSAGE:
                        CommandHeader header = ((RemotingCommand) msg).decodeCommandHeader(SendMessageRequestHeader.class);
                        byte[] body = ((RemotingCommand) msg).getBody();
                        Message message = new Message();
                        message.setBody(body);
                        msgs.put(i,message);
                        break;
                    case RequestCode.PULL_MESSAGE:
                        header = ((RemotingCommand) msg).decodeCommandHeader(PullMessageRequestHeader.class);
                        processPullMessage(ctx);
                        break;
                    default:
                        break;
                }
            }
        }

        private void processPullMessage(ChannelHandlerContext ctx) {
            Iterator<Map.Entry<Integer, Message>> iterator = msgs.entrySet().iterator();
            while (iterator.hasNext()){
                Map.Entry<Integer, Message> entry = iterator.next();
                Message message = entry.getValue();
                RemotingCommand remotingCommand = new RemotingCommand();
                remotingCommand.setBody(message.getBody());
                PullMessageResponseHeader responseHeader = new PullMessageResponseHeader();
                remotingCommand.setCustomHeader(responseHeader);
                ctx.writeAndFlush(remotingCommand);
                iterator.remove();
            }
        }
    }
}
