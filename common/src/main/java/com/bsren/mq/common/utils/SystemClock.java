package com.bsren.mq.common.utils;

public interface SystemClock {
    default long now(){
        return System.currentTimeMillis();
    }
}
