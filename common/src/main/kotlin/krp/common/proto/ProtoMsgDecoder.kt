@file:Suppress("DuplicatedCode")

package krp.common.proto

import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.LengthFieldBasedFrameDecoder
import io.netty.util.ReferenceCountUtil
import krp.common.proto.msg.*
import krp.common.utils.CryptoUtils
import krp.common.utils.injectLogger
import krp.common.utils.unGZip
import java.io.IOException
import kotlin.experimental.and

class ProtoMsgDecoder : LengthFieldBasedFrameDecoder(
    1024 * 1024, // maxFrameLength
    3, // lengthFieldOffset
    4, // lengthFieldLength
) {

    private val logger by injectLogger()

    override fun decode(ctx: ChannelHandlerContext?, `in`: ByteBuf?): Any? {
        val frame = super.decode(ctx, `in`)
        if (frame is ByteBuf) {
            val msg = tryDecodeProtoMsg(ctx, frame)
            ReferenceCountUtil.safeRelease(frame)
            return msg
        }
        return frame
    }

    private fun tryDecodeProtoMsg(ctx: ChannelHandlerContext?, frame: ByteBuf): ProtoMsg {
        val aes128Key = ctx?.channel()?.attr(Proto.AK_AES128_KEY)?.get()
        frame.skipBytes(2) // 1(HDR) + 1(VERSION)
        val flags = frame.readByte()
        frame.skipBytes(4) //  4(LENGTH)
        val typeValue = frame.readByte()
        return when (ProtoMsg.findType(typeValue)) {
            ProtoMsg.Type.Unknown -> ProtoMsgUnknown
            ProtoMsg.Type.Ping -> ProtoMsgPing
            ProtoMsg.Type.Pong -> ProtoMsgPong
            ProtoMsg.Type.Handshake -> {
                val data = ByteArray(frame.readInt())
                    .also { frame.readBytes(it) }
                    .tryUnGzip(flags)
                ProtoMsgHandshake(data, false)
            }
            ProtoMsg.Type.Request -> {
                val data = ByteArray(frame.readInt())
                    .also { frame.readBytes(it) }
                    .tryDecrypt(aes128Key, flags)
                    .tryUnGzip(flags)
                ProtoMsgRequest(data, null, false)
            }
            ProtoMsg.Type.Response -> {
                val status = frame.readByte() == 1.toByte()
                val tunnelId = frame.readLong()
                val data = ByteArray(frame.readInt())
                    .also { frame.readBytes(it) }
                    .tryDecrypt(aes128Key, flags)
                    .tryUnGzip(flags)
                ProtoMsgResponse(status, tunnelId, data, null, false)
            }
            ProtoMsg.Type.Transfer -> {
                val tunnelId = frame.readLong()
                val sessionId = frame.readLong()
                val data = ByteArray(frame.readInt())
                    .also { frame.readBytes(it) }
                    .tryDecrypt(aes128Key, flags)
                    .tryUnGzip(flags)
                ProtoMsgTransfer(tunnelId, sessionId, data, null, false)
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
                val data = ByteArray(frame.readInt()).also { frame.readBytes(it) }
                    .tryDecrypt(aes128Key, flags)
                    .tryUnGzip(flags)
                ProtoMsgRemoteConnected(tunnelId, sessionId, data, null, false)
            }
            ProtoMsg.Type.RemoteDisconnect -> {
                val tunnelId = frame.readLong()
                val sessionId = frame.readLong()
                val data = ByteArray(frame.readInt())
                    .also { frame.readBytes(it) }
                    .tryDecrypt(aes128Key, flags)
                    .tryUnGzip(flags)
                ProtoMsgRemoteDisconnect(tunnelId, sessionId, data, null, false)
            }
            ProtoMsg.Type.ForceOff -> ProtoMsgForceOff
            ProtoMsg.Type.ForceOffReply -> ProtoMsgForceOffReply
        }
    }

    @Throws(Exception::class)
    private fun ByteArray.tryDecrypt(aes128Key: ByteArray?, flags: Byte): ByteArray {
        return if (aes128Key != null && flags and Proto.FLAG_ENCRYPTED == Proto.FLAG_ENCRYPTED) {
            CryptoUtils.decryptAES128(this, aes128Key)
        } else {
            this
        }
    }

    @Throws(IOException::class)
    private fun ByteArray.tryUnGzip(flags: Byte): ByteArray {
        return if (flags and Proto.FLAG_GZIP == Proto.FLAG_GZIP) {
            val res = unGZip(this)
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

