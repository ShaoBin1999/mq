package com.bsren.mq.remote;

import com.alibaba.fastjson.JSON;
import com.bsren.mq.remote.exception.RemotingCommandException;
import com.bsren.mq.remote.protocol.RemotingSerializable;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Data
public class RemotingCommand {

    private static final Logger log = LoggerFactory.getLogger(RemotingCommand.class);

    private static final Map<Class<? extends CommandHeader>, Field[]> CLASS_HASH_MAP =
            new ConcurrentHashMap<>();

    private static final Map<Class, String> CANONICAL_NAME_CACHE = new ConcurrentHashMap<>();
    private static AtomicInteger requestId = new AtomicInteger(0);

    private int code;
    transient byte[] body;
    private int type;
    private String remark;
    private transient CommandHeader header;  //不会被序列化，内容存在fields里面
    private HashMap<String,String> fields;
    /**
     * 请求标识码。在Java版的通信层中，这个只是一个不断自增的整形，为了收到应答方响应的的时候找到对应的请求。
     *
     * flag： 按位(bit)解释。
     *
     * 第0位标识是这次通信是request还是response，0标识request, 1 标识response。
     *
     * 第1位标识是否是oneway请求，1标识oneway。应答方在处理oneway请求的时候，不会做出响应，请求方也无序等待应答方响应。
     */
    private int opaque = requestId.getAndIncrement();

    private static final String STRING_CANONICAL_NAME = String.class.getCanonicalName();
    private static final String DOUBLE_CANONICAL_NAME_1 = Double.class.getCanonicalName();
    private static final String DOUBLE_CANONICAL_NAME_2 = double.class.getCanonicalName();
    private static final String INTEGER_CANONICAL_NAME_1 = Integer.class.getCanonicalName();
    private static final String INTEGER_CANONICAL_NAME_2 = int.class.getCanonicalName();
    private static final String LONG_CANONICAL_NAME_1 = Long.class.getCanonicalName();
    private static final String LONG_CANONICAL_NAME_2 = long.class.getCanonicalName();
    private static final String BOOLEAN_CANONICAL_NAME_1 = Boolean.class.getCanonicalName();
    private static final String BOOLEAN_CANONICAL_NAME_2 = boolean.class.getCanonicalName();

    public static RemotingCommand createResponseCommand(int code, String remark) {
        return createResponseCommand(code, remark, null);
    }

    public static RemotingCommand createResponseCommand(int code, String remark,
                                                        Class<? extends CommandHeader> classHeader) {
        RemotingCommand cmd = new RemotingCommand();
        cmd.setCode(code);
        cmd.setRemark(remark);

        if (classHeader != null) {
            try {
                cmd.header = classHeader.newInstance();
            } catch (InstantiationException | IllegalAccessException e) {
                return null;
            }
        }
        return cmd;
    }


    public ByteBuffer encodeHeader(){
        return encodeHeader(this.body!=null?this.body.length:0);
    }

    public ByteBuffer encodeHeader(final int bodyLength) {
        int length = 4;
        byte[] headerData = this.headerEncode(this);
        length+=headerData.length;
        length+=bodyLength;
        ByteBuffer result = ByteBuffer.allocate(4 + length - bodyLength);
        result.putInt(length);
        result.put(markProtocolType(headerData.length));
        result.put(headerData);
        result.flip();
        return result;
    }


    public static int getHeaderLength(int length) {
        return length & 0xFFFFFF;
    }

    public static byte[] markProtocolType(int source) {
        byte[] result = new byte[4];

        result[0] = (byte) 0;
        result[1] = (byte) ((source >> 16) & 0xFF);
        result[2] = (byte) ((source >> 8) & 0xFF);
        result[3] = (byte) (source & 0xFF);
        return result;
    }



    byte[] headerEncode(Object obj) {
        String jsonString = JSON.toJSONString(obj);
        return jsonString.getBytes(StandardCharsets.UTF_8);
    }

    public static RemotingCommand decode(final ByteBuffer byteBuffer) {
        int length = byteBuffer.limit();
        int oriHeaderLen = byteBuffer.getInt();
        int headerLength = getHeaderLength(oriHeaderLen);

        byte[] headerData = new byte[headerLength];
        byteBuffer.get(headerData);

        RemotingCommand cmd = headerDecode(headerData);

        int bodyLength = length - 4 - headerLength;
        byte[] bodyData = null;
        if (bodyLength > 0) {
            bodyData = new byte[bodyLength];
            byteBuffer.get(bodyData);
        }
        cmd.body = bodyData;

        return cmd;
    }

    private static RemotingCommand headerDecode(byte[] headerData) {
        return RemotingSerializable.decode(headerData, RemotingCommand.class);
    }

    public CommandHeader decodeCommandHeader(Class<? extends CommandHeader> headerClass){
        CommandHeader commandHeader;
        try {
            commandHeader = headerClass.newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            return null;
        }
        if(this.fields==null){
            return commandHeader;
        }
        Field[] fields = getClazzFields(headerClass);
        for (Field field:fields){
            if(Modifier.isStatic(field.getModifiers())) {
                continue;
            }
            String fieldName = field.getName();
            try {
                String value = this.fields.get(fieldName);
                field.setAccessible(true);
                String type = CANONICAL_NAME_CACHE.computeIfAbsent(field.getType(), Class::getCanonicalName);
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
                    throw new RemotingCommandException("the custom field <" + fieldName + "> type is not supported");
                }
                field.set(commandHeader,valueParsed);

            }catch (Throwable e){
                log.error("Failed field [{}] decoding", fieldName, e);
            }
        }
        return commandHeader;
    }


    public static RemotingCommand createRequestCommand(int code,CommandHeader commandHeader){
        RemotingCommand command = new RemotingCommand();
        command.setCode(code);
        command.header = commandHeader;
        return command;
    }

    public void encodeHeaderToMap(){
        if(this.header==null){
            return;
        }


    }

    Field[] getClazzFields(Class<? extends CommandHeader> classHeader) {
        Field[] field = CLASS_HASH_MAP.get(classHeader);

        if (field == null) {
            Set<Field> fieldList = new HashSet<>();
            for (Class className = classHeader; className != Object.class; className = className.getSuperclass()) {
                Field[] fields = className.getDeclaredFields();
                fieldList.addAll(Arrays.asList(fields));
            }
            field = fieldList.toArray(new Field[0]);
            CLASS_HASH_MAP.put(classHeader,field);
        }
        return field;
    }




}
