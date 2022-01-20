package lighttunnel.base.proto

import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.LengthFieldBasedFrameDecoder

class ProtoMsgDecoder : LengthFieldBasedFrameDecoder(
    1024 * 1024, // maxFrameLength
    3, // lengthFieldOffset
    4, // lengthFieldLength
) {

    override fun decode(ctx: ChannelHandlerContext?, `in`: ByteBuf?): Any? {
        val frame = super.decode(ctx, `in`)
        if (frame is ByteBuf) {
            return tryDecodeProtoMsg(frame)
        }
        return frame
    }

    private fun tryDecodeProtoMsg(frame: ByteBuf): ProtoMsg {
        frame.skipBytes(7) // 1(HDR) + 1(VERSION) + 1(FLAGS) + 4(LENGTH)
        val typeValue = frame.readByte()
        return when (ProtoMsg.findType(typeValue)) {
            ProtoMsg.Type.Unknown -> ProtoMsgUnknown
            ProtoMsg.Type.Ping -> ProtoMsgPing
            ProtoMsg.Type.Pong -> ProtoMsgPong
            ProtoMsg.Type.Request -> {
                val dataSize = frame.readInt()
                val data = ByteArray(dataSize)
                frame.readBytes(data)
                ProtoMsgRequest(String(data))
            }
            ProtoMsg.Type.Response -> {
                val status = frame.readByte() == 1.toByte()
                val tunnelId = frame.readLong()
                val dataSize = frame.readInt()
                val data = ByteArray(dataSize)
                frame.readBytes(data)
                ProtoMsgResponse(status, tunnelId, String(data))
            }
            ProtoMsg.Type.Transfer -> {
                val tunnelId = frame.readLong()
                val sessionId = frame.readLong()
                val dataSize = frame.readInt()
                val data = ByteArray(dataSize)
                frame.readBytes(data)
                ProtoMsgTransfer(tunnelId, sessionId, data)
            }
            ProtoMsg.Type.LocalConnected -> {
                val tunnelId = frame.readLong()
                val sessionId = frame.readLong()
                ProtoMsgLocalConnected(tunnelId, sessionId)
            }
            ProtoMsg.Type.LocalDisconnect -> {
                val tunnelId = frame.readLong()
                val sessionId = frame.readLong()
                ProtoMsgLocalDisconnect(tunnelId, sessionId)
            }
            ProtoMsg.Type.RemoteConnected -> {
                val tunnelId = frame.readLong()
                val sessionId = frame.readLong()
                val dataSize = frame.readInt()
                val data = ByteArray(dataSize)
                ProtoMsgRemoteConnected(tunnelId, sessionId, String(data))
            }
            ProtoMsg.Type.RemoteDisconnect -> {
                val tunnelId = frame.readLong()
                val sessionId = frame.readLong()
                val dataSize = frame.readInt()
                val data = ByteArray(dataSize)
                ProtoMsgRemoteDisconnect(tunnelId, sessionId, String(data))
            }
            ProtoMsg.Type.ForceOff -> ProtoMsgForceOff
            ProtoMsg.Type.ForceOffReply -> ProtoMsgForceOffReply
        }
    }

}

