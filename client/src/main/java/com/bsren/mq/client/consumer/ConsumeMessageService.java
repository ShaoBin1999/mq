package com.bsren.mq.client.consumer;

import com.bsren.mq.common.Message;
import com.bsren.mq.remote.protocol.ConsumeMessageResult;

public interface ConsumeMessageService {

    void start();

    void shutdown();

    ConsumeMessageResult consumeMessage(Message message,String brokerName);



}
