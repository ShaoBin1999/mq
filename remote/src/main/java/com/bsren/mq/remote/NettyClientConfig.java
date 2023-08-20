package com.bsren.mq.remote;

public class NettyClientConfig {

    /**
     * worker thread number
     */
    private int clientWorkerThreads = 4;
    private int connectTimeoutMillis = 3000;
    private long channelNotActiveInterval = 1000 * 60;



    public long getConnectTimeoutMillis() {
        return 0;
    }
}
