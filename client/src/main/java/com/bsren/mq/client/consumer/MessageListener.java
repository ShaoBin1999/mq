package com.bsren.mq.client.consumer;

import com.bsren.mq.common.Message;

import java.util.List;

public interface MessageListener {

    ConsumeStatus consumeMessage(final List<Message> msgs,
                                        final ConsumeContext context);
}
