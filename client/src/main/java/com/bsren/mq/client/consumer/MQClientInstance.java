package com.bsren.mq.client.consumer;

public class MQClientInstance {


    private PullMessageService pullMessageService;
    private MQClientAPIImpl mQClientAPIImpl;

    public PullMessageService getPullMessageService() {
        return pullMessageService;
    }

    public MQClientAPIImpl getMQClientAPIImpl() {
        return mQClientAPIImpl;
    }
}
