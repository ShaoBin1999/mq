package com.bsren.mq.remote;

import com.bsren.mq.remote.config.NettyServerConfig;
import com.bsren.mq.remote.service.RemotingServer;
import com.bsren.mq.remote.service.RemotingService;
import com.bsren.mq.remote.utils.RemotingUtils;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.util.concurrent.DefaultEventExecutorGroup;
import javafx.util.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class NettyRemotingServer extends NettyRemotingAbstract implements RemotingServer {


    private static final Logger log = LoggerFactory.getLogger(NettyRemotingServer.class);

    private ServerBootstrap serverBootstrap;

    private EventLoopGroup eventSelector;

    private EventLoopGroup eventExecutors;

    private ChannelEventListener channelEventListener;

    private final NettyServerConfig nettyServerConfig;

    private final ExecutorService publicExecutor;

    private final Timer timer = new Timer("ServerHouseKeepingService", true);

    private DefaultEventExecutorGroup defaultEventExecutorGroup;

    private int port = 0;

    public NettyRemotingServer(final NettyServerConfig nettyServerConfig,
                               final ChannelEventListener channelEventListener) {
        this.serverBootstrap = new ServerBootstrap();
        this.nettyServerConfig = nettyServerConfig;
        this.channelEventListener = channelEventListener;
        int publicThreadNums = nettyServerConfig.getServerCallbackExecutorThreads();
        this.publicExecutor = Executors.newFixedThreadPool(publicThreadNums);
        this.eventExecutors = new NioEventLoopGroup(1);
        if(useEpoll()){
            this.eventSelector = new EpollEventLoopGroup(nettyServerConfig.getServerWorkerThreads());
        }else {
            this.eventSelector = new NioEventLoopGroup(nettyServerConfig.getServerWorkerThreads());
        }
    }


    private boolean useEpoll() {
        return RemotingUtils.isLinuxPlatform()
                && Epoll.isAvailable();
    }

    @Override
    public ExecutorService getCallbackExecutor() {
        return null;
    }

    @Override
    public void registerProcessor(int requestCode, NettyRequestProcessor processor, ExecutorService executor) {
        ExecutorService executorThis = executor;
        if (null == executor) {
            executorThis = this.publicExecutor;
        }

        Pair<NettyRequestProcessor, ExecutorService> pair = new Pair<NettyRequestProcessor, ExecutorService>(processor, executorThis);
        this.processorTable.put(requestCode, pair);
    }

    @Override
    public void registerDefaultProcessor(NettyRequestProcessor processor, ExecutorService executor) {
        this.defaultRequestProcessor = new Pair<>(processor, executor);
    }

    @Override
    public void start() {
        this.defaultEventExecutorGroup = new DefaultEventExecutorGroup(nettyServerConfig.getServerWorkerThreads());
        this.serverBootstrap.group(this.eventExecutors,this.eventSelector)
                .channel(NioServerSocketChannel.class)
                .localAddress(new InetSocketAddress(this.nettyServerConfig.getListenPort()))
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        ch.pipeline().addLast(defaultEventExecutorGroup,
                                new NettyEncoder(),
                                new NettyDecoder(),
                                new NettyServerHandler());
                    }
                });
        try {
            ChannelFuture sync = this.serverBootstrap.bind().sync();
            InetSocketAddress address = (InetSocketAddress) sync.channel().localAddress();
            this.port = address.getPort();
        }catch (InterruptedException e){
        }
        this.timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                try {
                    scanResponseTable();
                }catch (Throwable e){

                }
            }

        },1000*3,1000);
    }

    @Override
    public void shutdown() {
        this.timer.cancel();
        this.eventSelector.shutdownGracefully();
        this.eventExecutors.shutdownGracefully();
        this.defaultEventExecutorGroup.shutdownGracefully();
        this.publicExecutor.shutdown();;
    }

    class NettyServerHandler extends SimpleChannelInboundHandler<RemotingCommand>{

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, RemotingCommand msg) throws Exception {
            processMessageReceived(ctx, msg);
        }
    }

    /**
     * 过时的response删掉，执行回调方法
     */
    public void scanResponseTable(){
        List<ResponseFuture> timeoutList = new LinkedList<>();
        Iterator<Map.Entry<Integer, ResponseFuture>> iterator = this.responseTable.entrySet().iterator();
        while (iterator.hasNext()){
            Map.Entry<Integer, ResponseFuture> next = iterator.next();
            ResponseFuture response = next.getValue();
            //todo 可以配置的
            if(response.getBeginTimestamp()+ response.getTimeoutMills()+1000<System.currentTimeMillis()){
                iterator.remove();
                timeoutList.add(response);
            }
        }
        for (ResponseFuture future : timeoutList) {
            try {
                executeInvokeCallback(future);
            }catch (Throwable t){
                log.warn("scanResponseTable, operationComplete Exception", t);
            }
        }
    }
}
