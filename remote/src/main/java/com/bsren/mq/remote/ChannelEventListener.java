package com.bsren.mq.remote;

import io.netty.channel.Channel;

public interface ChannelEventListener {

    void onChannelConnect(String address, Channel channel);

    void onChannelClose(String address,Channel channel);
}
