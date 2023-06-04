package lighttunnel.common.proto

import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.LengthFieldBasedFrameDecoder
import io.netty.util.ReferenceCountUtil
import lighttunnel.common.extensions.injectLogger
import lighttunnel.common.proto.msg.*
import lighttunnel.common.utils.CompressUtils
import lighttunnel.common.utils.CryptoUtils
import java.io.IOException
import kotlin.experimental.and

class ProtoMsgDecoder : LengthFieldBasedFrameDecoder(
    maxFrameLength,
    lengthFieldOffset,
    lengthFieldLength,
    lengthAdjustment,
    initialBytesToStrip,
    failFast
) {

    private companion object {
        private const val maxFrameLength = 1024 * 1024
        private const val lengthFieldOffset = 3
        private const val lengthFieldLength = 4
        private const val lengthAdjustment = 0
        private const val initialBytesToStrip = 0
        private const val failFast = true
    }

    private val logger by injectLogger()

    override fun decode(ctx: ChannelHandlerContext?, `in`: ByteBuf?): Any? {
        val frame = super.decode(ctx, `in`)
        try {
            ctx ?: return frame
            return if (frame is ByteBuf) tryDecodeProtoMsg(ctx, frame) else frame
        } finally {
            ReferenceCountUtil.safeRelease(frame)
        }
    }

    private fun tryDecodeProtoMsg(ctx: ChannelHandlerContext, frame: ByteBuf): ProtoMsg {
        val aes128Key = ctx.channel().attr(Proto.AK_AES128_KEY).get()
        frame.skipBytes(2) // 1(HDR) + 1(VERSION)
        val flag = frame.readByte()
        frame.skipBytes(4) //  4(LENGTH)
        val typeValue = frame.readByte()
        return when (ProtoMsg.findCmdByValue(typeValue)) {
            ProtoMsg.Cmd.Unknown -> ProtoMsgUnknown
            ProtoMsg.Cmd.Ping -> ProtoMsgPing
            ProtoMsg.Cmd.Pong -> ProtoMsgPong
            ProtoMsg.Cmd.Handshake -> {
                val data = ByteArray(frame.readInt())
                    .also { frame.readBytes(it) }
                    .tryUnGzip(flag)
                ProtoMsgHandshake(data, false)
            }

            ProtoMsg.Cmd.Request -> {
                val data = ByteArray(frame.readInt())
                    .also { frame.readBytes(it) }
                    .tryDecrypt(aes128Key, flag)
                    .tryUnGzip(flag)
                ProtoMsgRequest(data, null, false)
            }

            ProtoMsg.Cmd.Response -> {
                val status = frame.readByte() == 1.toByte()
                val tunnelId = frame.readLong()
                val data = ByteArray(frame.readInt())
                    .also { frame.readBytes(it) }
                    .tryDecrypt(aes128Key, flag)
                    .tryUnGzip(flag)
                ProtoMsgResponse(status, tunnelId, data, null, false)
            }

            ProtoMsg.Cmd.Transfer -> {
                val tunnelId = frame.readLong()
                val sessionId = frame.readLong()
                val data = ByteArray(frame.readInt())
                    .also { frame.readBytes(it) }
                    .tryDecrypt(aes128Key, flag)
                    .tryUnGzip(flag)
                ProtoMsgTransfer(tunnelId, sessionId, data, null, false)
            }

            ProtoMsg.Cmd.LocalConnected -> {
                val tunnelId = frame.readLong()
                val sessionId = frame.readLong()
                ProtoMsgLocalConnected(tunnelId, sessionId)
            }

            ProtoMsg.Cmd.LocalDisconnect -> {
                val tunnelId = frame.readLong()
                val sessionId = frame.readLong()
                ProtoMsgLocalDisconnect(tunnelId, sessionId)
            }

            ProtoMsg.Cmd.RemoteConnected -> {
                val tunnelId = frame.readLong()
                val sessionId = frame.readLong()
                val data = ByteArray(frame.readInt()).also { frame.readBytes(it) }
                    .tryDecrypt(aes128Key, flag)
                    .tryUnGzip(flag)
                ProtoMsgRemoteConnected(tunnelId, sessionId, data, null, false)
            }

            ProtoMsg.Cmd.RemoteDisconnect -> {
                val tunnelId = frame.readLong()
                val sessionId = frame.readLong()
                val data = ByteArray(frame.readInt())
                    .also { frame.readBytes(it) }
                    .tryDecrypt(aes128Key, flag)
                    .tryUnGzip(flag)
                ProtoMsgRemoteDisconnect(tunnelId, sessionId, data, null, false)
            }

            ProtoMsg.Cmd.ForceOff -> ProtoMsgForceOff
            ProtoMsg.Cmd.ForceOffReply -> ProtoMsgForceOffReply
        }
    }

    @Throws(Exception::class)
    private fun ByteArray.tryDecrypt(aes128Key: ByteArray?, flag: Byte): ByteArray {
        return if (aes128Key != null && flag and Proto.FLAG_ENCRYPTED == Proto.FLAG_ENCRYPTED) {
            CryptoUtils.decryptAES128(this, aes128Key)
        } else {
            this
        }
    }

    @Throws(IOException::class)
    private fun ByteArray.tryUnGzip(flag: Byte): ByteArray {
        return if (flag and Proto.FLAG_GZIP == Proto.FLAG_GZIP) {
            val res = CompressUtils.decompress(this)
            logger.debug(
                "Hit gzip: before: {}, after: {}, ratio: {}",
                res.size,
                this.size,
                this.size.toDouble() / res.size.toDouble()
            )
            res
        } else {
            this
        }
    }

}

