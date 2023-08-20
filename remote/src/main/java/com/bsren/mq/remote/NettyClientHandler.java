package com.bsren.mq.remote;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import static com.bsren.mq.remote.Constant.REQUEST_COMMAND;
import static com.bsren.mq.remote.Constant.RESPONSE_COMMAND;

public class NettyClientHandler extends SimpleChannelInboundHandler<RemotingCommand> {

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, RemotingCommand msg) throws Exception {
        processMessageReceived(ctx, msg);
    }

    public void processMessageReceived(ChannelHandlerContext ctx, RemotingCommand msg) throws Exception {
        if (msg != null) {
            switch (msg.getType()) {
                case REQUEST_COMMAND:
                    processRequestCommand(ctx, msg);
                    break;
                case RESPONSE_COMMAND:
                    processResponseCommand(ctx, msg);
                    break;
                default:
                    break;
            }
        }
    }

    private void processResponseCommand(ChannelHandlerContext ctx, RemotingCommand cmd) {

    }

    private void processRequestCommand(ChannelHandlerContext ctx, RemotingCommand cmd) {

    }
}
