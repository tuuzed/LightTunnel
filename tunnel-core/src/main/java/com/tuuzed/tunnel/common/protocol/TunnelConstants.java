package com.tuuzed.tunnel.common.protocol;

import io.netty.channel.Channel;
import io.netty.util.AttributeKey;


public final class TunnelConstants {
    /**
     * 消息帧域长度
     */
    public static final int MESSAGE_FRAME_FIELD_SIZE = 4;
    /**
     * 消息类型域长度
     */
    public static final int MESSAGE_TYPE_FIELD_SIZE = 1;
    /**
     * 消息头部域长度
     */
    public static final int MESSAGE_HEAD_FIELD_SIZE = 4;
    /**
     * 空的字节数组
     */
    public static final byte[] EMPTY_BYTES = new byte[0];

    /**
     * 心跳消息
     */
    public static final byte MESSAGE_TYPE_HEARTBEAT = 0x01;
    /**
     * 建立隧道请求
     */
    public static final byte MESSAGE_TYPE_OPEN_TUNNEL_REQUEST = 0x02;
    /**
     * 建立隧道响应
     */
    public static final byte MESSAGE_TYPE_OPEN_TUNNEL_RESPONSE = 0x03;
    /**
     * 透传消息
     */
    public static final byte MESSAGE_TYPE_TRANSFER = 0x04;
    /**
     * 连接本地隧道
     */
    public static final byte MESSAGE_TYPE_CONNECT_LOCAL_TUNNEL = 0x05;
    /**
     * 本地隧道已连接
     */
    public static final byte MESSAGE_TYPE_LOCAL_TUNNEL_CONNECTED = 0x06;
    /**
     * 本地隧道断开连接
     */
    public static final byte MESSAGE_TYPE_LOCAL_TUNNEL_DISCONNECT = 0x07;


    public static final AttributeKey<Channel> ATTR_NEXT_CHANNEL = AttributeKey.newInstance("next_channel");

    public static final AttributeKey<String> ATTR_MAPPING = AttributeKey.newInstance("mapping");
    public static final AttributeKey<String> ATTR_LOCAL_ADDR = AttributeKey.newInstance("local_addr");
    public static final AttributeKey<Integer> ATTR_LOCAL_PORT = AttributeKey.newInstance("local_port");
    public static final AttributeKey<Integer> ATTR_REMOTE_PORT = AttributeKey.newInstance("remote_port");
    public static final AttributeKey<Long> ATTR_TUNNEL_TOKEN = AttributeKey.newInstance("tunnel_token");
    public static final AttributeKey<Long> ATTR_SESSION_TOKEN = AttributeKey.newInstance("session_token");


}
