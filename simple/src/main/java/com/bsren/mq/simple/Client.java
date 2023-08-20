package com.bsren.mq.simple;

import com.bsren.mq.simple.message.Message;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;

import java.net.InetSocketAddress;
import java.net.UnknownHostException;

public class Client {

    public static void main(String[] args) throws UnknownHostException, InterruptedException {
        Client client = new Client();
        client.start();
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
                    }
                })
                .connect(new InetSocketAddress("localhost",Server.PORT)).sync();
        sync.channel().writeAndFlush("haha");
        Channel channel = sync.channel();
        for (int i=0;i<100;i++){
            RemotingCommand remotingCommand = new RemotingCommand();
            remotingCommand.setCode(RequestCode.SEND_MESSAGE);
            byte[] bytes = new byte[10];
            bytes[0] = (byte) i;
            remotingCommand.setBody(bytes);
            int finalI = i;
            channel.writeAndFlush(remotingCommand).addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture channelFuture) throws Exception {
                    if(channelFuture.isSuccess()){
                        System.out.println("第"+ finalI +"message send success");
                    }
                }
            });
        }
    }





}
