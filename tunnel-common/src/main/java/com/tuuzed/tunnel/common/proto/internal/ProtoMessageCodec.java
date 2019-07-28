package com.tuuzed.tunnel.common.proto.internal;

import com.tuuzed.tunnel.common.proto.ProtoMessage;
import io.netty.buffer.ByteBuf;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 协议消息编码解码器
 */
public final class ProtoMessageCodec {
    /**
     * 消息帧域长度
     */
    public static final int MESSAGE_FRAME_FIELD_SIZE = 4;
    /**
     * 消息类型域长度
     */
    private static final int MESSAGE_TYPE_FIELD_SIZE = 1;
    /**
     * 消息头部域长度
     */
    private static final int MESSAGE_HEAD_FIELD_SIZE = 4;

    public static void encode(@NotNull ProtoMessage msg, @NotNull ByteBuf out) throws Exception {
        int messageFrameLength = MESSAGE_TYPE_FIELD_SIZE + MESSAGE_HEAD_FIELD_SIZE;
        messageFrameLength += msg.getHead().length;
        messageFrameLength += msg.getData().length;
        // 消息帧长度
        out.writeInt(messageFrameLength);
        // 消息类型
        out.writeByte(msg.getType().getValue());
        // 消息头长度与消息头数据
        out.writeInt(msg.getHead().length);
        out.writeBytes(msg.getHead());
        // 消息数据
        out.writeBytes(msg.getData());
    }

    @Nullable
    public static ProtoMessage decode(ByteBuf in) throws Exception {
        if (in == null) {
            return null;
        }
        // 判断可读数据是否满足读取消息帧长度添加
        if (in.readableBytes() < MESSAGE_FRAME_FIELD_SIZE) {
            return null;
        }
        int messageFrameLength = in.readInt();
        if (in.readableBytes() < messageFrameLength) {
            return null;
        }
        ProtoMessage.Type type = ProtoMessage.Type.of(in.readByte());
        int headLength = in.readInt();
        byte[] head = new byte[headLength];
        in.readBytes(head);
        int dataLength = messageFrameLength
                - MESSAGE_TYPE_FIELD_SIZE
                - MESSAGE_HEAD_FIELD_SIZE
                - headLength;
        byte[] data = new byte[dataLength];
        in.readBytes(data);
        return new ProtoMessage(type, head, data);
    }

}
