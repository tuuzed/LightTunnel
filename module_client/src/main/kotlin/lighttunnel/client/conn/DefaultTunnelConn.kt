package lighttunnel.client.conn

import io.netty.bootstrap.Bootstrap
import io.netty.channel.ChannelFuture
import io.netty.channel.ChannelFutureListener
import lighttunnel.client.consts.AK_RSA_PRI_KEY
import lighttunnel.client.consts.AK_TUNNEL_CONN
import lighttunnel.client.consts.AK_TUNNEL_REQUEST
import lighttunnel.common.entity.TunnelRequest
import lighttunnel.common.extensions.emptyBytes
import lighttunnel.common.extensions.injectLogger
import lighttunnel.common.extensions.tryCompress
import lighttunnel.common.proto.msg.ProtoMsgHandshake
import lighttunnel.common.utils.CryptoUtils
import java.util.concurrent.atomic.AtomicBoolean

internal class DefaultTunnelConn(
    override val serverIp: String,
    override val serverPort: Int,
    private val originalTunnelRequest: TunnelRequest,
    private val useEncryption: Boolean,
    private val bootstrap: Bootstrap,
) : TunnelConn {
    private val logger by injectLogger()

    override val tunnelRequest get() = finalTunnelRequest ?: originalTunnelRequest

    private var openChannelFuture: ChannelFuture? = null

    var finalTunnelRequest: TunnelRequest? = null

    private val activeClosedFlag = AtomicBoolean(false)

    val isActiveClosed get() = activeClosedFlag.get()

    fun open(failure: (conn: DefaultTunnelConn) -> Unit) {
        if (isActiveClosed) {
            logger.warn("This tunnel already closed.")
            return
        }
        bootstrap.connect(serverIp, serverPort)
            .also { openChannelFuture = it }
            .addListener(ChannelFutureListener { future ->
                if (!future.isSuccess) {
                    failure(this)
                    return@ChannelFutureListener
                }
                val channel = future.channel()
                channel.attr(AK_TUNNEL_REQUEST).set(tunnelRequest)
                channel.attr(AK_TUNNEL_CONN).set(this)
                // 连接成功，向服务器发送握手消息
                if (useEncryption) {
                    val rsaPriAndPubKey = CryptoUtils.randomRSAKeyPair()
                    val rsaPriKey = rsaPriAndPubKey.first
                    val rsaPubKey = rsaPriAndPubKey.second
                    channel.attr(AK_RSA_PRI_KEY).set(rsaPriKey)
                    val compressedAndData = rsaPubKey.tryCompress()
                    channel.writeAndFlush(ProtoMsgHandshake(compressedAndData.second, compressedAndData.first))
                } else {
                    channel.writeAndFlush(ProtoMsgHandshake(emptyBytes, false))
                }
            })
    }

    fun close() {
        activeClosedFlag.set(true)
        openChannelFuture?.apply {
            channel().attr(AK_TUNNEL_CONN).set(null)
            channel().close()
        }
    }

    override fun toString(): String = tunnelRequest.toString(serverIp)

}
