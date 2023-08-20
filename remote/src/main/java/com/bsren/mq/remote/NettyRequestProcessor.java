package com.bsren.mq.remote;

import io.netty.channel.ChannelHandlerContext;

public interface NettyRequestProcessor {

    RemotingCommand processRequest(ChannelHandlerContext ctx,RemotingCommand remotingCommand) throws Exception;

    boolean rejectRequest();

}
