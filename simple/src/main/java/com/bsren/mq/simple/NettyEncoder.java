package com.bsren.mq.simple;

import com.bsren.mq.simple.utils.RemotingUtils;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.Arrays;

public class NettyEncoder extends MessageToByteEncoder<RemotingCommand> {

    private static final Logger log = LoggerFactory.getLogger(NettyEncoder.class);

    @Override
    protected void encode(ChannelHandlerContext channelHandlerContext, RemotingCommand remotingCommand, ByteBuf byteBuf) throws Exception {
        try {
            ByteBuffer buffer= remotingCommand.encode();
            byteBuf.writeBytes(buffer);
        }catch (Exception e){
            log.error("encode exception");
            RemotingUtils.closeChannel(channelHandlerContext.channel());
        }
    }
}
