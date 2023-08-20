package com.bsren.mq.remote;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;


public class NettyDecoder extends LengthFieldBasedFrameDecoder {

    private static final Logger log = LoggerFactory.getLogger(NettyDecoder.class);
    private static final int FRAME_MAX_LENGTH = 16777216;

    public NettyDecoder() {
        super(FRAME_MAX_LENGTH, 0, 4, 0, 4);
    }

    @Override
    protected Object decode(ChannelHandlerContext ctx, ByteBuf in) throws Exception {
        ByteBuf frame = null;
        try {
            frame = (ByteBuf) super.decode(ctx,in);
            if(frame==null){
                return null;
            }
            ByteBuffer byteBuf = frame.nioBuffer();
            return RemotingCommand.decode(byteBuf);
        }catch (Exception e){
            log.error("",e);
        }finally {
            if(frame!=null){
                frame.release();
            }
        }
        return null;
    }
}
