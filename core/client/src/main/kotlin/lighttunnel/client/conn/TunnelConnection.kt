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

class TunnelConnection internal constructor(
    internal val serverAddr: String,
    internal val serverPort: Int,
    private val tunnelRequest: TunnelRequest,
    internal val sslContext: SslContext? = null
) {

    private val logger by loggerDelegate()

    internal val request get() = finalTunnelRequest ?: tunnelRequest

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
                    val head = request.toBytes()
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

    override fun toString(): String = request.toString(serverAddr)

}