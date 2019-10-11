package com.tuuzed.lighttunnel.common

import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.LengthFieldBasedFrameDecoder

class LTMassageDecoder : LengthFieldBasedFrameDecoder(
    4 * 1024 * 1024,
    0,
    TP_MESSAGE_LENGTH_FIELD_LENGTH,
    0,
    0,
    true
) {
    companion object {
        private const val MIN_BYTES = TP_MESSAGE_LENGTH_FIELD_LENGTH +
                TP_MESSAGE_COMMAND_LENGTH +
                TP_MESSAGE_HEAD_LENGTH_FIELD_LENGTH
    }

    @Throws(Exception::class)
    override fun decode(ctx: ChannelHandlerContext?, _inbuf: ByteBuf?): Any? {
        val inbuf = super.decode(ctx, _inbuf)
        if (inbuf is ByteBuf) {
            if (inbuf.readableBytes() < MIN_BYTES) return null
            val msgTotalLength = inbuf.readInt()
            if (inbuf.readableBytes() < msgTotalLength) return null
            // 开始解码数据
            val cmd = LTCommand.ofValue(inbuf.readByte())
            val headLength = inbuf.readInt()
            val head = ByteArray(headLength)
            inbuf.readBytes(head)
            val dataLength = msgTotalLength -
                    TP_MESSAGE_COMMAND_LENGTH -
                    TP_MESSAGE_HEAD_LENGTH_FIELD_LENGTH -
                    headLength
            val data = ByteArray(dataLength)
            inbuf.readBytes(data)
            return LTMassage(cmd, head, data)
        } else {
            return inbuf
        }
    }

}