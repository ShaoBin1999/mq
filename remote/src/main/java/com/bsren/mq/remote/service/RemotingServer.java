package com.bsren.mq.remote.service;

import com.bsren.mq.remote.NettyRequestProcessor;

import java.util.concurrent.ExecutorService;

public interface RemotingServer extends RemotingService{

    //针对某个特定的请求注册处理器以及线程池
    void registerProcessor(final int requestCode, final NettyRequestProcessor processor,
                           final ExecutorService executor);

    void registerDefaultProcessor(final NettyRequestProcessor processor, final ExecutorService executor);


}
