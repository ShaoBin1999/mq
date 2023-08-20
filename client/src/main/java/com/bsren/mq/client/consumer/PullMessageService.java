package com.bsren.mq.client.consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.*;

public class PullMessageService implements Runnable{

    private static final Logger log = LoggerFactory.getLogger(PullMessageService.class);

    private volatile boolean stopped = false;
    private final LinkedBlockingQueue<PullRequest> pullRequestQueue = new LinkedBlockingQueue<PullRequest>();

    private final ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();


    DefaultConsumer defaultConsumer;

    @Override
    public void run() {
        while (!this.stopped){
            PullRequest request = this.pullRequestQueue.poll();
            if(request!=null){
                pullMessage(request);
            }
        }
    }


    private void pullMessage(PullRequest request){
        defaultConsumer.pullMessage(request);
    }

    public void executePullRequestLater(PullRequest request, long timeDelay) {
        this.scheduledExecutorService.schedule(new Runnable() {
            @Override
            public void run() {
                executePullRequestImmediately(request);
            }
        },timeDelay, TimeUnit.SECONDS);
    }


    public void executePullRequestImmediately(final PullRequest pullRequest) {
        try {
            this.pullRequestQueue.put(pullRequest);
        } catch (InterruptedException e) {
            log.error("executePullRequestImmediately pullRequestQueue.put", e);
        }
    }
}
