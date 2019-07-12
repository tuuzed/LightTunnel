package com.tuuzed.tunnel.common.protocol;

import org.jetbrains.annotations.NotNull;

public final class TunnelMessage {
    /**
     * 消息类型
     */
    private byte type;
    /**
     * 消息头
     */
    private byte[] head;
    /**
     * 消息数据
     */
    private byte[] data;

    @NotNull
    public static TunnelMessage newInstance(byte type) {
        return new TunnelMessage(type);
    }

    private TunnelMessage(byte type) {
        this.type = type;
    }

    public byte getType() {
        return type;
    }

    @NotNull
    public byte[] getHead() {
        if (head == null) {
            return TunnelConstants.EMPTY_BYTES;
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
            return TunnelConstants.EMPTY_BYTES;
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
        String type;
        switch (this.type) {
            case TunnelConstants.MESSAGE_TYPE_HEARTBEAT_PING:
                type = "HEARTBEAT_PING";
                break;
            case TunnelConstants.MESSAGE_TYPE_HEARTBEAT_PONG:
                type = "HEARTBEAT_PONG";
                break;
            case TunnelConstants.MESSAGE_TYPE_OPEN_TUNNEL_REQUEST:
                type = "OPEN_TUNNEL_REQUEST";
                break;
            case TunnelConstants.MESSAGE_TYPE_OPEN_TUNNEL_RESPONSE:
                type = "OPEN_TUNNEL_RESPONSE";
                break;
            case TunnelConstants.MESSAGE_TYPE_CONNECT_LOCAL_TUNNEL:
                type = "CONNECT_LOCAL_TUNNEL";
                break;
            case TunnelConstants.MESSAGE_TYPE_TRANSFER:
                type = "TRANSFER";
                break;
            case TunnelConstants.MESSAGE_TYPE_LOCAL_TUNNEL_CONNECTED:
                type = "LOCAL_TUNNEL_CONNECTED";
                break;
            case TunnelConstants.MESSAGE_TYPE_LOCAL_TUNNEL_DISCONNECT:
                type = "LOCAL_TUNNEL_DISCONNECT";
                break;
            default:
                type = String.valueOf(this.type);
                break;
        }
        return "TunnelMessage(Type: " + type + ")";
    }

}
