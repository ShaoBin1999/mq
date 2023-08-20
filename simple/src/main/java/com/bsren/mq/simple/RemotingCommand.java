package com.bsren.mq.simple;

import com.bsren.mq.simple.utils.RemotingSerializable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;


public class RemotingCommand {

    private static final Logger log = LoggerFactory.getLogger(RemotingCommand.class);


    private static final AtomicInteger requestId = new AtomicInteger(0);
    private static final String STRING_CANONICAL_NAME = String.class.getCanonicalName();
    private static final String DOUBLE_CANONICAL_NAME_1 = Double.class.getCanonicalName();
    private static final String DOUBLE_CANONICAL_NAME_2 = double.class.getCanonicalName();
    private static final String INTEGER_CANONICAL_NAME_1 = Integer.class.getCanonicalName();
    private static final String INTEGER_CANONICAL_NAME_2 = int.class.getCanonicalName();
    private static final String LONG_CANONICAL_NAME_1 = Long.class.getCanonicalName();
    private static final String LONG_CANONICAL_NAME_2 = long.class.getCanonicalName();
    private static final String BOOLEAN_CANONICAL_NAME_1 = Boolean.class.getCanonicalName();
    private static final String BOOLEAN_CANONICAL_NAME_2 = boolean.class.getCanonicalName();

    private int code;

    private int opaque = requestId.getAndIncrement();
    private HashMap<String, String> extFields;
    private transient CommandHeader customHeader;

    private transient byte[] body;

    protected RemotingCommand() {
    }



    public static RemotingCommand decode(final ByteBuffer byteBuffer){
        int length = byteBuffer.getInt();
        int headerLen = byteBuffer.getInt();
        byte[] headerData = new byte[headerLen];
        byteBuffer.get(headerData);
        RemotingCommand cmd = headerDecode(headerData);
        int bodyLen = length-4-headerLen;
        if(bodyLen>0){
            byte[] body = new byte[bodyLen];
            byteBuffer.get(body);
            cmd.body = body;
        }
        return cmd;
    }

    private byte[] headerEncode(){
        this.moveHeaderToExtFields();
        return RemotingSerializable.encode(this);
    }

    private void moveHeaderToExtFields() {
        if(this.customHeader==null){
            return;
        }
        Field[] fields = customHeader.getClass().getDeclaredFields();
        for (Field field : fields) {
            if(Modifier.isStatic(field.getModifiers())){
                continue;
            }
            String name = field.getName();
            try {
                field.setAccessible(true);
                Object o = field.get(this.customHeader);
                if(o!=null){
                    this.extFields.put(name,o.toString());
                }
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
    }

    private static RemotingCommand headerDecode(byte[] headerData) {
        return RemotingSerializable.decode(headerData, RemotingCommand.class);
    }

    public ByteBuffer encode(){
        int length = 4;
        byte[] headerData = this.headerEncode();
        length+=headerData.length;
        if(this.body!=null){
            length+=body.length;
        }
        ByteBuffer result = ByteBuffer.allocate(4+length);
        result.putInt(length);
        result.putInt(headerData.length);
        result.put(headerData);
        if(this.body!=null){
            result.put(this.body);
        }
        result.flip();
        return result;
    }


    public CommandHeader decodeCommandHeader(Class<? extends CommandHeader> classHeader) {
        CommandHeader commandHeader;
        try {
            commandHeader = classHeader.newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            return null;
        }
        if (this.extFields == null) {
            return commandHeader;
        }
        Field[] fields = classHeader.getDeclaredFields();
        for (Field field : fields) {
            if (!Modifier.isStatic(field.getModifiers())) {
                String fieldName = field.getName();
                try {
                    String value = this.extFields.get(fieldName);
                    if (null == value) {
                        continue;
                    }

                    field.setAccessible(true);
                    String type = field.getType().getCanonicalName();
                    Object valueParsed;

                    if (type.equals(STRING_CANONICAL_NAME)) {
                        valueParsed = value;
                    } else if (type.equals(INTEGER_CANONICAL_NAME_1) || type.equals(INTEGER_CANONICAL_NAME_2)) {
                        valueParsed = Integer.parseInt(value);
                    } else if (type.equals(LONG_CANONICAL_NAME_1) || type.equals(LONG_CANONICAL_NAME_2)) {
                        valueParsed = Long.parseLong(value);
                    } else if (type.equals(BOOLEAN_CANONICAL_NAME_1) || type.equals(BOOLEAN_CANONICAL_NAME_2)) {
                        valueParsed = Boolean.parseBoolean(value);
                    } else if (type.equals(DOUBLE_CANONICAL_NAME_1) || type.equals(DOUBLE_CANONICAL_NAME_2)) {
                        valueParsed = Double.parseDouble(value);
                    } else {
                        throw new Exception("the custom field <" + fieldName + "> type is not supported");
                    }

                    field.set(commandHeader, valueParsed);

                } catch (Throwable e) {
                    log.error("Failed field [{}] decoding", fieldName, e);
                }
            }
        }
        return commandHeader;
    }

    public void setBody(byte[] body) {
        this.body = body;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public byte[] getBody() {
        return body;
    }

    public int getCode() {
        return code;
    }

    @Override
    public String toString() {
        return "RemotingCommand{" +
                "code=" + code +
                ", opaque=" + opaque +
                ", extFields=" + extFields +
                ", customHeader=" + customHeader +
                ", body=" + Arrays.toString(body) +
                '}';
    }
}
