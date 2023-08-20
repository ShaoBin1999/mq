package com.bsren.mq.client.consumer;


import com.bsren.mq.common.protocol.RequestCode;
import com.bsren.mq.remote.NettyRequestProcessor;
import com.bsren.mq.remote.RemotingCommand;
import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClientRemotingProcessor implements NettyRequestProcessor {

    private final Logger log = LoggerFactory.getLogger(ClientRemotingProcessor.class);

    private final MQClientInstance mqClientFactory;

    public ClientRemotingProcessor(MQClientInstance mqClientFactory){
        this.mqClientFactory = mqClientFactory;
    }




    @Override
    public RemotingCommand processRequest(ChannelHandlerContext ctx, RemotingCommand remotingCommand) throws Exception {
        switch (remotingCommand.getCode()){
            case RequestCode.CONSUME_MESSAGE_DIRECTLY:
                return this.consumeMessageDirectly(ctx, remotingCommand);
            default:
                break;
        }
        return null;
    }

    private RemotingCommand consumeMessageDirectly(ChannelHandlerContext ctx, RemotingCommand remotingCommand) {
        return null;
    }

    @Override
    public boolean rejectRequest() {
        return false;
    }
}
