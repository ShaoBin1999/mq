package com.bsren.mq.client.consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PullAPIWrapper {

    private final static Logger log = LoggerFactory.getLogger(PullAPIWrapper.class);


    private final MQClientInstance mQClientFactory;

    public PullAPIWrapper(MQClientInstance instance){
        this.mQClientFactory = instance;
    }

    public PullResult pullKernelApi(
            String brokerAddress,
            PullCallback pullCallback,
            long timeoutMills
    ){
        PullMessageRequestHeader requestHeader = new PullMessageRequestHeader();
        return this.mQClientFactory.getMQClientAPIImpl().pullMessage(
                brokerAddress,
                requestHeader,
                timeoutMills,
                pullCallback
        );
    }
}
