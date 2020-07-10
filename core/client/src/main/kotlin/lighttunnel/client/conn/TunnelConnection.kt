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

class TunnelConnection(
    val serverAddr: String,
    val serverPort: Int,
    private val tunnelRequest: TunnelRequest,
    internal val sslContext: SslContext? = null
) {
    private val logger by loggerDelegate()
    private var openChannelFuture: ChannelFuture? = null

    val originalTunnelRequest get() = tunnelRequest
    var finalTunnelRequest: TunnelRequest? = null
        internal set

    private val activeClosedFlag = AtomicBoolean(false)
    val isActiveClosed get() = activeClosedFlag.get()

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
                    val head = (finalTunnelRequest ?: originalTunnelRequest).toBytes()
                    future.channel().writeAndFlush(ProtoMessage(ProtoMessageType.REQUEST, head = head))
                    future.channel().attr(AK_TUNNEL_CONNECTION).set(this)
                } else {
                    failure.invoke(this)
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

    override fun toString(): String {
        return (finalTunnelRequest ?: originalTunnelRequest).toString(serverAddr)
    }


}