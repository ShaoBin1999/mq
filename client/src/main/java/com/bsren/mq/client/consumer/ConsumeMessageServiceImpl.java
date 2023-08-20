package com.bsren.mq.client.consumer;

import com.bsren.mq.common.CMResult;
import com.bsren.mq.common.Message;
import com.bsren.mq.remote.protocol.ConsumeMessageResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class ConsumeMessageServiceImpl implements ConsumeMessageService {

    private static final Logger log = LoggerFactory.getLogger(ConsumeMessageServiceImpl.class);

    private final ScheduledExecutorService scheduledExecutorService;

    private final MessageListener messageListener;

    public ConsumeMessageServiceImpl(MessageListener messageListener){
        this.messageListener = messageListener;
        this.scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
    }


    public void start(){

    }

    @Override
    public void shutdown() {

    }

    @Override
    public ConsumeMessageResult consumeMessage(Message message, String brokerName) {
        ConsumeMessageResult result = new ConsumeMessageResult();
        List<Message> msg = new ArrayList<>();
        msg.add(message);
        ConsumeContext context = new ConsumeContext();
        try {
            ConsumeStatus status = this.messageListener.consumeMessage(msg,context);
            if(status!=null){
                switch (status){
                    case SUCCESS:
                        result.setCmResult(CMResult.CR_SUCCESS);
                        break;
                    default:
                        break;
                }
            }else {
                result.setCmResult(CMResult.CR_RETURN_NULL);
            }
        }catch (Exception e){
            log.info("");
        }
        return result;
    }


}
