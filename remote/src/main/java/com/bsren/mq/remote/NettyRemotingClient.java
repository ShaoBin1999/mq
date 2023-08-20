package com.bsren.mq.remote;

import com.bsren.mq.remote.service.RemotingClient;
import com.bsren.mq.remote.utils.RemotingUtils;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.concurrent.DefaultEventExecutorGroup;
import io.netty.bootstrap.Bootstrap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class NettyRemotingClient implements RemotingClient {

    private static final Logger log = LoggerFactory.getLogger(NettyRemotingClient.class);

    private final NettyClientConfig nettyClientConfig;
    private static final long LOCK_TIMEOUT_MILLIS = 3000;
    private ExecutorService callbackExecutor;
    private DefaultEventExecutorGroup defaultEventExecutorGroup;
    private final Bootstrap bootstrap = new Bootstrap();
    private final EventLoopGroup eventLoopGroupWorker =  new NioEventLoopGroup(1);
    private final ConcurrentMap<String /* addr */, ChannelFuture> channelTables = new ConcurrentHashMap<>();
    private final Lock lockChannelTables = new ReentrantLock();

    public NettyRemotingClient(NettyClientConfig nettyClientConfig){
        this.nettyClientConfig = nettyClientConfig;
    }








    @Override
    public RemotingCommand invoke(String address, RemotingCommand remotingCommand) {
        try {
            Channel channel = getOrCreateChannel(address);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return null;
    }

    private Channel getOrCreateChannel(String address) throws InterruptedException {
        ChannelFuture channelFuture = channelTables.get(address);
        if(channelFuture!=null && channelFuture.channel()!=null && channelFuture.channel().isActive()){
            return channelFuture.channel();
        }
        return createChannel(address);
    }

    private Channel createChannel(String address) throws InterruptedException {
        ChannelFuture channelFuture = this.channelTables.get(address);
        if(channelFuture!=null){
            Channel channel = channelFuture.channel();
            if(channel!=null && channel.isActive()){
                return channel;
            }
        }
        if(this.lockChannelTables.tryLock(LOCK_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)){
            try {
                boolean createNewChannel;
                channelFuture = this.channelTables.get(address);
                if(channelFuture!=null){
                    Channel ch = channelFuture.channel();
                    if(ch!=null && ch.isActive()){
                        return ch;
                    }else if(!channelFuture.isDone()){
                        createNewChannel = false;
                    }else {
                        this.channelTables.remove(address);
                        createNewChannel = true;
                    }
                }else {
                    createNewChannel = true;
                }
                if(createNewChannel){
                    channelFuture = this.bootstrap.connect(RemotingUtils.string2SocketAddress(address));
                    this.channelTables.put(address,channelFuture);
                }
            }catch (Exception e){
                log.error("createChannel: create channel exception", e);
            }finally {
                this.lockChannelTables.unlock();
            }
        }else {
            log.warn("createChannel: try to lock channel table, but timeout, {}ms", LOCK_TIMEOUT_MILLIS);
        }
        if(channelFuture!=null){
            if(channelFuture.awaitUninterruptibly(nettyClientConfig.getConnectTimeoutMillis())){
                Channel channel = channelFuture.channel();
                if(channel!=null && channel.isActive()){
                    log.info("createChannel: connect remote host[{}] success, {}", address, channelFuture);
                    return channel;
                }else {
                    log.warn("createChannel: connect remote host[" + address + "] failed, " + channelFuture, channelFuture.cause());
                }
            }else {
                log.warn("createChannel: connect remote host[{}] timeout {}ms, {}", address, this.nettyClientConfig.getConnectTimeoutMillis(),
                        channelFuture);
            }
        }
        return null;
    }


    @Override
    public void start() {
//        this.defaultEventExecutorGroup = new DefaultEventExecutorGroup(5);
//        this.bootstrap.group(this.eventLoopGroupWorker).channel(NioSocketChannel.class)
//                .handler(new ChannelInitializer<SocketChannel>() {
//                    @Override
//                    protected void initChannel(SocketChannel ch) throws Exception {
//                        ChannelPipeline pipeline = ch.pipeline();
//                        pipeline.addLast(defaultEventExecutorGroup,
//                                )
//                    }
//                })
    }

    @Override
    public void shutdown() {

    }
}
