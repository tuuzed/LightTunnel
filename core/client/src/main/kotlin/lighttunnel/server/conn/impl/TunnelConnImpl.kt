package lighttunnel.server.conn.impl

import io.netty.bootstrap.Bootstrap
import io.netty.channel.ChannelFuture
import io.netty.channel.ChannelFutureListener
import io.netty.handler.ssl.SslContext
import lighttunnel.base.TunnelRequest
import lighttunnel.base.proto.ProtoMessage
import lighttunnel.base.utils.loggerDelegate
import lighttunnel.server.conn.TunnelConn
import lighttunnel.server.utils.AK_TUNNEL_CONN
import java.util.concurrent.atomic.AtomicBoolean

internal class TunnelConnImpl(
    override val serverAddr: String,
    override val serverPort: Int,
    private val originalTunnelRequest: TunnelRequest,
    val sslContext: SslContext?
) : TunnelConn {
    private val logger by loggerDelegate()

    override val tunnelRequest get() = finalTunnelRequest ?: originalTunnelRequest

    private var openChannelFuture: ChannelFuture? = null

    var finalTunnelRequest: TunnelRequest? = null

    private val activeClosedFlag = AtomicBoolean(false)

    val isActiveClosed get() = activeClosedFlag.get()

    fun open(bootstrap: Bootstrap, failure: (conn: TunnelConnImpl) -> Unit) {
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
                    future.channel().writeAndFlush(ProtoMessage.REQUEST(tunnelRequest))
                    future.channel().attr(AK_TUNNEL_CONN).set(this)
                } else {
                    failure(this)
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

    override fun toString(): String = tunnelRequest.toString(serverAddr)

}
