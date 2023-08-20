package com.bsren.mq.common;

import lombok.Data;

import java.io.Serializable;
import java.util.Map;

@Data
public class Message implements Serializable {
    private static final long serialVersionUID = 8445773977080406428L;

    String topic;

    private Map<String,String> properties;

    private byte[] body;

    public Message() {}

    private String tags;

    public Message(String topic,String tags,byte[] body){
        this.topic = topic;
        this.tags = tags;
        this.body = body;
    }
}
