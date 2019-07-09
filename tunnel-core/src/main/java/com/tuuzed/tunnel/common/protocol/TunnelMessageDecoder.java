package com.tuuzed.tunnel.common.protocol;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;

/**
 * Tunnel消息解码器
 */
public class TunnelMessageDecoder extends LengthFieldBasedFrameDecoder {

    public TunnelMessageDecoder() {
        this(4 * 1024 * 1024);
    }

    public TunnelMessageDecoder(int maxFrameLength) {
        super(maxFrameLength, 0, TunnelConstants.MESSAGE_FRAME_FIELD_SIZE, 0, 0);
    }

    @Override
    protected Object decode(ChannelHandlerContext ctx, ByteBuf formalParameterIn) throws Exception {
        ByteBuf in = (ByteBuf) super.decode(ctx, formalParameterIn);
        if (in == null) {
            return null;
        }
        // 判断可读数据是否满足读取消息帧长度添加
        if (in.readableBytes() < TunnelConstants.MESSAGE_FRAME_FIELD_SIZE) {
            return null;
        }
        int messageFrameLength = in.readInt();
        if (in.readableBytes() < messageFrameLength) {
            return null;
        }
        byte type = in.readByte();
        int headLength = in.readInt();
        byte[] head = new byte[headLength];
        in.readBytes(head);
        int dataLength = messageFrameLength
                - TunnelConstants.MESSAGE_TYPE_FIELD_SIZE
                - TunnelConstants.MESSAGE_HEAD_FIELD_SIZE
                - headLength;
        byte[] data = new byte[dataLength];
        in.readBytes(data);
        in.release();
        return TunnelMessage.newInstance(type)
                .setHead(head)
                .setData(data)
                ;
    }


}
