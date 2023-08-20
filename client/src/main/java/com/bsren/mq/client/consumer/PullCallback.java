package com.bsren.mq.client.consumer;

public interface PullCallback {

    void onSuccess(PullResult result);

    void onException(Throwable e);


}
