package lighttunnel.client.conn

import io.netty.bootstrap.Bootstrap
import io.netty.channel.ChannelFuture
import io.netty.channel.ChannelFutureListener
import io.netty.handler.ssl.SslContext
import lighttunnel.client.util.AK_TUNNEL_CONNECTION
import lighttunnel.logger.loggerDelegate
import lighttunnel.proto.ProtoMessage
import lighttunnel.proto.ProtoMessageType
import lighttunnel.proto.TunnelRequest
import java.util.concurrent.atomic.AtomicBoolean

@Suppress("MemberVisibilityCanBePrivate")
class TunnelConnection private constructor(
    val serverAddr: String,
    val serverPort: Int,
    private val originalTunnelRequest: TunnelRequest,
    internal val sslContext: SslContext?
) {
    private val logger by loggerDelegate()

    @Suppress("ClassName")
    internal companion object `-Companion` {
        fun newInstance(
            serverAddr: String,
            serverPort: Int,
            tunnelRequest: TunnelRequest,
            sslContext: SslContext? = null
        ) = TunnelConnection(serverAddr, serverPort, tunnelRequest, sslContext)
    }

    val tunnelRequest get() = finalTunnelRequest ?: originalTunnelRequest

    private var openChannelFuture: ChannelFuture? = null

    internal var finalTunnelRequest: TunnelRequest? = null

    private val activeClosedFlag = AtomicBoolean(false)

    internal val isActiveClosed get() = activeClosedFlag.get()

    internal fun open(bootstrap: Bootstrap, failure: (conn: TunnelConnection) -> Unit) {
        if (isActiveClosed) {
            logger.warn("This tunnel already closed.")
            return
        }
        @Suppress("RedundantSamConstructor")
        bootstrap.connect(serverAddr, serverPort)
            .also { openChannelFuture = it }
            .addListener(ChannelFutureListener { future ->
                if (future.isSuccess) {
                    // 连接成功，向服务器发送请求建立隧道消息
                    val head = originalTunnelRequest.toBytes()
                    future.channel().writeAndFlush(ProtoMessage(ProtoMessageType.REQUEST, head = head))
                    future.channel().attr(AK_TUNNEL_CONNECTION).set(this)
                } else {
                    failure(this)
                }
            })
    }

    internal fun close() {
        activeClosedFlag.set(true)
        openChannelFuture?.apply {
            channel().attr(AK_TUNNEL_CONNECTION).set(null)
            channel().close()
        }
    }

    override fun toString(): String = tunnelRequest.toString(serverAddr)

}