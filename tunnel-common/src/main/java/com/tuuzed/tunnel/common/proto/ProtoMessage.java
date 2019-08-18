package com.tuuzed.tunnel.common.proto;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ProtoMessage {
    @NotNull
    private static final byte[] EMPTY_BYTES = new byte[0];

    @NotNull
    private Type type;
    @Nullable
    private byte[] head;
    @Nullable
    private byte[] data;

    public ProtoMessage(@NotNull Type type, @Nullable byte[] head, @Nullable byte[] data) {
        this.type = type;
        this.head = head;
        this.data = data;
    }

    @NotNull
    public Type getType() {
        return type;
    }

    @NotNull
    public byte[] getHead() {
        if (head == null) {
            return EMPTY_BYTES;
        }
        return head;
    }

    @NotNull
    public byte[] getData() {
        if (data == null) {
            return EMPTY_BYTES;
        }
        return data;
    }

    @Override
    public String toString() {
        return "ProtoMessage{" +
            "type=" + type +
            ", head=head(" + getHead().length + ")" +
            ", data=data(" + getData().length + ")" +
            '}';
    }


    public enum Type {
        /**
         * 未知类型
         */
        UNKNOWN((byte) 0x00),
        /**
         * 心跳消息 PING
         * 消息流向：Client <-> Server
         */
        HEARTBEAT_PING((byte) 0x01),
        /**
         * 心跳消息 PONG
         * 消息流向：Client <-> Server
         */
        HEARTBEAT_PONG((byte) 0x02),
        /**
         * 建立隧道请求
         * 消息head域内容为OpenTunnelRequest数据
         * 消息流向：Client -> Server
         */
        REQUEST((byte) 0x03),
        /**
         * 建立隧道响应
         * 消息head域第1个字节为是否成功，剩下8个字节为Long类型的tunnelToken
         * 如果建立隧道成功data域为OpenTunnelRequest数据，如果建立隧道失败data域为失败消息
         * 消息流向：Client <- Server
         */
        RESPONSE((byte) 0x04),
        /**
         * 透传消息
         * 消息head域前8个字节为Long类型的tunnelToken，后8个字节为Long类型的sessionToken
         * 消息流向：Client <-> Server
         */
        TRANSFER((byte) 0x05),
        /**
         * 远程连接成功
         * 消息head域前8个字节为Long类型的tunnelToken，后8个字节为Long类型的sessionToken
         * 消息流向：Client <- Server
         */
        REMOTE_CONNECTED((byte) 0x06),
        /**
         * 远程断开连接
         * 消息head域前8个字节为Long类型的tunnelToken，后8个字节为Long类型的sessionToken
         * 消息流向：Client <- Server
         */
        REMOTE_DISCONNECT((byte) 0x07),
        /**
         * 本地连接成功
         * 消息head域前8个字节为Long类型的tunnelToken，后8个字节为Long类型的sessionToken
         * 消息流向：Client -> Server
         */
        LOCAL_CONNECTED((byte) 0x08),
        /**
         * 本地连接断开
         * 消息head域前8个字节为Long类型的tunnelToken，后8个字节为Long类型的sessionToken
         * 消息流向：Client -> Server
         */
        LOCAL_DISCONNECT((byte) 0x09),
        ;

        private byte value;

        Type(byte value) {
            this.value = value;
        }

        public byte getValue() {
            return value;
        }

        @NotNull
        public static Type of(byte value) {
            final Type[] values = values();
            for (Type type : values) {
                if (type.value == value) {
                    return type;
                }
            }
            return Type.UNKNOWN;
        }
    }


}
