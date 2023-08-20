package com.bsren.mq.simple.message;

import java.nio.ByteBuffer;

public class MessageCoder {

    public static final int MAGIC_CODE = 24;


    public static byte[] encodeMessage(Message message){
        byte[] body = message.getBody();
        int bodyLen = body.length;
        //totalSize(int) + magicCode + bodySize(int) + body
        int storeSize = 4+4+4+4+bodyLen;
        ByteBuffer buffer = ByteBuffer.allocate(storeSize);
        buffer.putInt(storeSize);
        buffer.putInt(MAGIC_CODE);
        buffer.putInt(bodyLen);
        buffer.put(body);
        return buffer.array();
    }

    public static Message decodeMessage(ByteBuffer buffer){
        Message message = new Message();
        int storeSize = buffer.getInt();
        int magicCode = buffer.getInt();
        assert magicCode == MAGIC_CODE;
        int bodyLen = buffer.getInt();
        byte[] body = new byte[bodyLen];
        buffer.get(body);
        message.setBody(body);
        return message;
    }
}
