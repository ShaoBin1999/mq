package com.bsren.mq.remote;

import com.bsren.mq.remote.exception.RemotingSendRequestException;
import com.bsren.mq.remote.exception.RemotingTimeoutException;
import com.bsren.mq.remote.protocol.ConsumeMessageResult;
import com.bsren.mq.remote.protocol.RemotingSysResponseCode;
import com.bsren.mq.remote.utils.RemotingUtils;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import javafx.util.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.SocketAddress;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;

import static com.bsren.mq.remote.Constant.REQUEST_COMMAND;
import static com.bsren.mq.remote.Constant.RESPONSE_COMMAND;

public abstract class NettyRemotingAbstract {

    public static final Logger log = LoggerFactory.getLogger(RemotingUtils.ROCKETMQ_REMOTING);

    //cache all on-going requests
    protected final ConcurrentMap<Integer,ResponseFuture> responseTable =
            new ConcurrentHashMap<>(256);

    protected final HashMap<Integer/* request code */, Pair<NettyRequestProcessor, ExecutorService>> processorTable =
            new HashMap<>(64);



    protected Pair<NettyRequestProcessor, ExecutorService> defaultRequestProcessor;

    public NettyRemotingAbstract(){

    }

    public void processMessageReceived(ChannelHandlerContext ctx,RemotingCommand remotingCommand){
        if(remotingCommand==null){
            return;
        }
        if (remotingCommand.getType() == REQUEST_COMMAND) {
            processRequestCommand(ctx, remotingCommand);
        } else if (remotingCommand.getType() == RESPONSE_COMMAND) {
            processResponseCommand(ctx, remotingCommand);
        }
    }

    private void processResponseCommand(ChannelHandlerContext ctx, RemotingCommand remotingCommand) {
        final Pair<NettyRequestProcessor, ExecutorService> matched = processorTable.get(remotingCommand.getCode());
        final Pair<NettyRequestProcessor,ExecutorService> pair = matched==null?this.defaultRequestProcessor:matched;
        final int opaque = remotingCommand.getOpaque();
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                try {
                    RemotingCommand response = pair.getKey().processRequest(ctx, remotingCommand);
                    if(response!=null){
                        response.setOpaque(opaque);
                        try {
                            ctx.writeAndFlush(response);
                        }catch (Throwable e){
                            log.error("process request over, but response failed", e);
                            log.error(response.toString());
                            log.error(response.toString());
                        }
                    }
                } catch (Exception e) {
                    log.error("process request exception", e);
                    log.error(remotingCommand.toString());
                    //todo 返回码
                    final RemotingCommand response = new RemotingCommand();
                    response.setOpaque(opaque);
                    ctx.writeAndFlush(response);
                }
            }
        };
        if(pair.getKey().rejectRequest()){
            RemotingCommand res = new RemotingCommand();
            res.setOpaque(opaque);
            ctx.writeAndFlush(res);
            return;
        }
        try {
            RequestTask requestTask = new RequestTask(runnable, ctx.channel(), remotingCommand);
            pair.getValue().submit(requestTask);
        }catch (RejectedExecutionException e){
            final RemotingCommand response = RemotingCommand.createResponseCommand(RemotingSysResponseCode.SYSTEM_BUSY,
                    "[OVERLOAD]system busy, start flow control for a while");
            response.setOpaque(opaque);
            ctx.writeAndFlush(response);
        }

    }

    private void processRequestCommand(ChannelHandlerContext ctx, RemotingCommand cmd) {
        final int opaque = cmd.getOpaque();
        final ResponseFuture responseFuture = responseTable.get(opaque);
        if(responseFuture!=null){
            responseFuture.setResponseCommand(cmd);
            responseTable.remove(opaque);
            if(responseFuture.getInvokeCallback()!=null){
                executeInvokeCallback(responseFuture);
            }else {
                responseFuture.putResponse(cmd);
            }
        }else {
            log.warn(cmd.toString());
        }
    }
    public abstract ExecutorService getCallbackExecutor();


    protected void executeInvokeCallback(ResponseFuture responseFuture) {
        boolean runInThisThread = false;
        ExecutorService executor = getCallbackExecutor();
        if(executor!=null && !executor.isShutdown()){
            try {
                executor.submit(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            responseFuture.executeInvokeCallback();
                        }catch (Throwable e){
                            log.warn("execute callback in executor exception, and callback throw", e);
                        }
                    }
                });
            }catch (Exception e){
                runInThisThread = true;
                log.warn("execute callback in executor exception, maybe executor busy", e);
            }
        }else {
            runInThisThread = true;
        }
        if(runInThisThread){
            try {
                responseFuture.executeInvokeCallback();
            }catch (Throwable t){
                log.warn("executeInvokeCallback Exception", t);
            }
        }
    }

    public RemotingCommand invokeSyncImpl(Channel channel,RemotingCommand request,long timeoutMills) throws InterruptedException, RemotingTimeoutException, RemotingSendRequestException {
        final int opaque = request.getOpaque();
        try {
            final ResponseFuture responseFuture = new ResponseFuture(opaque,timeoutMills,null);
            responseTable.put(opaque,responseFuture);
            SocketAddress address = channel.remoteAddress();
            channel.writeAndFlush(request).addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture channelFuture) throws Exception {
                    if(channelFuture.isSuccess()){
                        responseFuture.setSendRequestOK(true);
                        return;
                    }else {
                        responseFuture.setSendRequestOK(false);
                    }
                    responseTable.remove(opaque);
                    responseFuture.setCause(channelFuture.cause());
                    responseFuture.putResponse(null);
                    log.warn("send a request command to channel <" + address + "> failed.");
                }
            });
            RemotingCommand remotingCommand = responseFuture.waitResponse(timeoutMills);
            if(remotingCommand==null){
                if (responseFuture.isSendRequestOK()) {
                    throw new RemotingTimeoutException(RemotingUtils.parseSocketAddressAddr(address), timeoutMills,
                            responseFuture.getCause());
                } else {
                    throw new RemotingSendRequestException(RemotingUtils.parseSocketAddressAddr(address), responseFuture.getCause());
                }
            }
            return remotingCommand;
        }
        finally {
            this.responseTable.remove(opaque);
        }
    }

}
