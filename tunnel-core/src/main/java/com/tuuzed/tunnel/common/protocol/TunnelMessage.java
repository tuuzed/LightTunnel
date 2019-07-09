package com.tuuzed.tunnel.common.protocol;

import org.jetbrains.annotations.NotNull;

public class TunnelMessage {
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
            case TunnelConstants.MESSAGE_TYPE_HEARTBEAT:
                type = "MESSAGE_TYPE_HEARTBEAT";
                break;
            case TunnelConstants.MESSAGE_TYPE_REQUEST_OPEN_TUNNEL:
                type = "MESSAGE_TYPE_REQUEST_OPEN_TUNNEL";
                break;
            case TunnelConstants.MESSAGE_TYPE_TRANSFER:
                type = "MESSAGE_TYPE_TRANSFER";
                break;
            default:
                type = String.valueOf(this.type);
                break;
        }
        return "TunnelMessage(Type: " + type + ")";
    }

}
