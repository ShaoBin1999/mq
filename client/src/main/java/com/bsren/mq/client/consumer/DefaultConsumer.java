package com.bsren.mq.client.consumer;

import com.bsren.mq.remote.service.RemotingClient;

public class DefaultConsumer {

    private String consumerGroup;

    private String nameSrvAddress;

    private RemotingClient remotingClient;

    private MQClientInstance mQClientFactory;

    public DefaultConsumer(String  consumerGroup){
        this.consumerGroup = consumerGroup;
    }




    public void start(){

    }


    public void pullMessage(PullRequest request) {

    }

    private void executePullRequestLater(PullRequest request,long timeDelay){
        this.mQClientFactory.getPullMessageService().executePullRequestLater(request, timeDelay);
    }
}
