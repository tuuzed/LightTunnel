package com.tuuzed.tunnel.common.protocol;

import io.netty.channel.Channel;
import io.netty.util.AttributeKey;

import java.nio.charset.Charset;


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
     * 心跳消息 PING
     */
    public static final byte MESSAGE_TYPE_HEARTBEAT_PING = 0x01;
    /**
     * 心跳消息 PONG
     */
    public static final byte MESSAGE_TYPE_HEARTBEAT_PONG = 0x02;
    /**
     * 建立隧道请求
     */
    public static final byte MESSAGE_TYPE_OPEN_TUNNEL_REQUEST = 0x03;
    /**
     * 建立隧道响应
     */
    public static final byte MESSAGE_TYPE_OPEN_TUNNEL_RESPONSE = 0x04;
    /**
     * 建立隧道错误
     */
    public static final byte MESSAGE_TYPE_OPEN_TUNNEL_ERROR = 0x05;
    /**
     * 透传消息
     */
    public static final byte MESSAGE_TYPE_TRANSFER = 0x06;
    /**
     * 连接本地隧道
     */
    public static final byte MESSAGE_TYPE_CONNECT_LOCAL_TUNNEL = 0x07;
    /**
     * 本地隧道断开连接
     */
    public static final byte MESSAGE_TYPE_LOCAL_TUNNEL_DISCONNECT = 0x08;
    /**
     * 用户隧道断开连接
     */
    public static final byte MESSAGE_TYPE_USER_TUNNEL_DISCONNECT = 0x09;


    public static final AttributeKey<Channel> ATTR_NEXT_CHANNEL = AttributeKey.newInstance("next_channel");
    public static final AttributeKey<Boolean> ATTR_OPEN_TUNNEL_ERROR_FLAG = AttributeKey.newInstance("open_tunnel_error_flag");
    public static final AttributeKey<String> ATTR_OPEN_TUNNEL_ERROR_MESSAGE = AttributeKey.newInstance("open_tunnel_error_message");
    public static final AttributeKey<OpenTunnelRequest> ATTR_OPEN_TUNNEL_REQUEST = AttributeKey.newInstance("open_tunnel_request");
    public static final AttributeKey<Long> ATTR_TUNNEL_TOKEN = AttributeKey.newInstance("tunnel_token");
    public static final AttributeKey<Long> ATTR_SESSION_TOKEN = AttributeKey.newInstance("session_token");


}
