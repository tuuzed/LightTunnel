package com.tuuzed.tunnel.common.protocol;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import com.tuuzed.tunnel.common.logging.Logger;
import com.tuuzed.tunnel.common.logging.LoggerFactory;

/**
 * Tunnel消息编码器
 */
public class TunnelMessageEncoder extends MessageToByteEncoder<TunnelMessage> {

    private static final Logger logger = LoggerFactory.getLogger(TunnelMessageEncoder.class);

    @Override
    protected void encode(ChannelHandlerContext ctx, TunnelMessage msg, ByteBuf out) throws Exception {
        int messageFrameLength = TunnelConstants.MESSAGE_TYPE_FIELD_SIZE
                + TunnelConstants.MESSAGE_HEAD_FIELD_SIZE;
        messageFrameLength += msg.getHead().length;
        messageFrameLength += msg.getData().length;
        // 消息帧长度
        out.writeInt(messageFrameLength);
        // 消息类型
        out.writeByte(msg.getType());
        // 消息头长度与消息头数据
        out.writeInt(msg.getHead().length);
        out.writeBytes(msg.getHead());
        // 消息数据
        out.writeBytes(msg.getData());
    }

}
