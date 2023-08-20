package com.bsren.mq.remote.config;

import lombok.Data;

@Data
public class NettyServerConfig implements Cloneable{

    private int listenPort = 8888;

    private int serverWorkerThreads = 8;

    private int serverCallbackExecutorThreads = 4;

    private int serverChannelMaxIdleTimeSeconds = 120;

    private int serverSocketSndBufSize = 65535;

    private int serverSocketRcvBufSize = 65535;



    @Override
    public Object clone() throws CloneNotSupportedException {
        return (NettyServerConfig) super.clone();
    }

}
