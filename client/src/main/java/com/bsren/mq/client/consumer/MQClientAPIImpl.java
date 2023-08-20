package com.bsren.mq.client.consumer;

import com.bsren.mq.common.protocol.RequestCode;
import com.bsren.mq.common.protocol.ResponseCode;
import com.bsren.mq.remote.NettyRemotingClient;
import com.bsren.mq.remote.service.RemotingClient;
import com.bsren.mq.remote.RemotingCommand;


public class MQClientAPIImpl {
    private final RemotingClient remotingClient;

    public MQClientAPIImpl(){
        this.remotingClient = new NettyRemotingClient(null);
    }

    public PullResult pullMessage(String brokerAddress,
                                  PullMessageRequestHeader requestHeader,
                                  long timeoutMills,
                                  PullCallback pullCallback) {
        RemotingCommand request = new RemotingCommand();
        request.setHeader(requestHeader);
        request.setCode(RequestCode.PULL_MESSAGE);
        return pullMessageSync(brokerAddress,request,timeoutMills);
    }

    private PullResult pullMessageSync(String brokerAddress, RemotingCommand request, long timeoutMills) {
        RemotingCommand response = this.remotingClient.invoke(brokerAddress, request);
        PullResult result = new PullResult();
        switch (response.getCode()){
            case ResponseCode.SUCCESS:
                result.setPullStatus(PullStatus.FOUND);
                break;
            default:
                break;
        }
        return result;
    }

}
