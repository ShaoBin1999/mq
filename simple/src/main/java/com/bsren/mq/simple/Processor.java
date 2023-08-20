package com.bsren.mq.simple;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.concurrent.EventExecutorGroup;

import java.net.InetSocketAddress;
import java.net.UnknownHostException;

public class Processor {

    public static void main(String[] args) throws UnknownHostException, InterruptedException {
        Processor processor = new Processor();
        processor.start();
    }


    public void start() throws UnknownHostException, InterruptedException {
        Bootstrap bootstrap = new Bootstrap();
        EventLoopGroup eventExecutors = new NioEventLoopGroup();
        ChannelFuture sync = bootstrap.group(eventExecutors)
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<NioSocketChannel>() {
                    @Override
                    protected void initChannel(NioSocketChannel ch) throws Exception {
                        ch.pipeline().addLast(new NettyEncoder());
                        ch.pipeline().addLast(new NettyDecoder());
                        ch.pipeline().addLast(new NettyClientHandler());
                    }
                })
                .connect(new InetSocketAddress("localhost",Server.PORT)).sync();
        Channel channel = sync.channel();
        RemotingCommand remotingCommand = new RemotingCommand();
        remotingCommand.setCode(RequestCode.PULL_MESSAGE);
        channel.writeAndFlush(remotingCommand).addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture channelFuture) throws Exception {
                if(channelFuture.isSuccess()){
                    System.out.println("pull message send success");
                }
            }
        });
    }

    private static class NettyClientHandler extends ChannelInboundHandlerAdapter {
        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            System.out.println("接受消息"+msg);
        }
    }
}
