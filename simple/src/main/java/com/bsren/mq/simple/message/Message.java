package com.bsren.mq.simple.message;

import lombok.Data;

import java.io.Serializable;
import java.util.Map;

@Data
public class Message implements Serializable {

    private String topic;
    private byte[] body;
}
