package com.tuuzed.tunnel.common.protocol;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

public class TunnelMessage {

    @NotNull
    private static final byte[] EMPTY_BYTES = new byte[0];
    /**
     * 心跳消息 PING
     * 消息流向：Client <-> Server
     */
    public static final byte MESSAGE_TYPE_HEARTBEAT_PING = 0x01;
    /**
     * 心跳消息 PONG
     * 消息流向：Client <-> Server
     */
    public static final byte MESSAGE_TYPE_HEARTBEAT_PONG = 0x02;
    /**
     * 建立隧道请求
     * 消息head域内容为OpenTunnelRequest数据
     * 消息流向：Client -> Server
     */
    public static final byte MESSAGE_TYPE_OPEN_TUNNEL_REQUEST = 0x03;
    /**
     * 建立隧道响应
     * 消息head域第1个字节为是否成功，剩下8个字节为Long类型的tunnelToken
     * 如果建立隧道成功data域为OpenTunnelRequest数据，如果建立隧道失败data域为失败消息
     * 消息流向：Client <- Server
     */
    public static final byte MESSAGE_TYPE_OPEN_TUNNEL_RESPONSE = 0x04;
    /**
     * 建立隧道成功
     */
    public static final byte OPEN_TUNNEL_RESPONSE_FAILURE = 0x00;
    /**
     * 建立隧道失败
     */
    public static final byte OPEN_TUNNEL_RESPONSE_SUCCESS = 0x01;

    /**
     * 透传消息
     * 消息head域前8个字节为Long类型的tunnelToken，后8个字节为Long类型的sessionToken
     * 消息流向：Client <-> Server
     */
    public static final byte MESSAGE_TYPE_TRANSFER = 0x05;
    /**
     * 用户隧道连接成功
     * 消息head域前8个字节为Long类型的tunnelToken，后8个字节为Long类型的sessionToken
     * 消息流向：Client <- Server
     */
    public static final byte MESSAGE_TYPE_USER_TUNNEL_CONNECTED = 0x06;
    /**
     * 用户隧道断开连接
     * 消息head域前8个字节为Long类型的tunnelToken，后8个字节为Long类型的sessionToken
     * 消息流向：Client <- Server
     */
    public static final byte MESSAGE_TYPE_USER_TUNNEL_DISCONNECT = 0x07;
    /**
     * 本地连接成功
     * 消息head域前8个字节为Long类型的tunnelToken，后8个字节为Long类型的sessionToken
     * 消息流向：Client -> Server
     */
    public static final byte MESSAGE_TYPE_LOCAL_CONNECT_CONNECTED = 0x08;
    /**
     * 本地连接断开
     * 消息head域前8个字节为Long类型的tunnelToken，后8个字节为Long类型的sessionToken
     * 消息流向：Client -> Server
     */
    public static final byte MESSAGE_TYPE_LOCAL_CONNECT_DISCONNECT = 0x09;


    @NotNull
    public static TunnelMessage newInstance(byte type) {
        return new TunnelMessage(type);
    }

    /**
     * 消息类型
     */
    private byte type;
    /**
     * 消息头
     */
    @Nullable
    private byte[] head;
    /**
     * 消息数据
     */
    @Nullable
    private byte[] data;

    private TunnelMessage(byte type) {
        this.type = type;
    }

    public byte getType() {
        return type;
    }

    @NotNull
    public TunnelMessage setType(byte type) {
        this.type = type;
        return this;
    }

    @NotNull
    public byte[] getHead() {
        if (head == null) {
            return EMPTY_BYTES;
        }
        return head;
    }

    @NotNull
    public TunnelMessage setHead(byte[] head) {
        this.head = head;
        return this;
    }

    @NotNull
    public byte[] getData() {
        if (data == null) {
            return EMPTY_BYTES;
        }
        return data;
    }

    @NotNull
    public TunnelMessage setData(byte[] data) {
        this.data = data;
        return this;
    }

    @Override
    public String toString() {
        return getTypeName();
//        return "TunnelMessage{" +
//                "type=" + getTypeName() +
//                ", head=" + Arrays.toString(getHead()) +
//                ", data=" + Arrays.toString(getData()) +
//                '}';
    }

    @NotNull
    private String getTypeName() {
        switch (type) {
            case MESSAGE_TYPE_HEARTBEAT_PING:
                return "HEARTBEAT_PING";
            case MESSAGE_TYPE_HEARTBEAT_PONG:
                return "HEARTBEAT_PONG";
            case MESSAGE_TYPE_OPEN_TUNNEL_REQUEST:
                return "OPEN_TUNNEL_REQUEST";
            case MESSAGE_TYPE_OPEN_TUNNEL_RESPONSE:
                return "OPEN_TUNNEL_RESPONSE";
            case MESSAGE_TYPE_TRANSFER:
                return "TRANSFER";
            case MESSAGE_TYPE_USER_TUNNEL_CONNECTED:
                return "USER_TUNNEL_CONNECTED";
            case MESSAGE_TYPE_USER_TUNNEL_DISCONNECT:
                return "USER_TUNNEL_DISCONNECT";
            case MESSAGE_TYPE_LOCAL_CONNECT_CONNECTED:
                return "LOCAL_CONNECT_CONNECTED";
            case MESSAGE_TYPE_LOCAL_CONNECT_DISCONNECT:
                return "LOCAL_CONNECT_DISCONNECT";
            default:
                return String.valueOf(type);
        }
    }
}
