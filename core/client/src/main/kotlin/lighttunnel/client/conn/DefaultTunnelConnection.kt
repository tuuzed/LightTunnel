package lighttunnel.client.conn

import io.netty.bootstrap.Bootstrap
import io.netty.channel.ChannelFuture
import io.netty.channel.ChannelFutureListener
import io.netty.handler.ssl.SslContext
import lighttunnel.client.openapi.conn.TunnelConnection
import lighttunnel.client.util.AK_TUNNEL_CONNECTION
import lighttunnel.logger.loggerDelegate
import lighttunnel.proto.ProtoMessage
import lighttunnel.proto.ProtoMessageType
import lighttunnel.proto.TunnelRequest
import java.util.concurrent.atomic.AtomicBoolean

internal class DefaultTunnelConnection(
    override val serverAddr: String,
    override val serverPort: Int,
    private val originalTunnelRequest: TunnelRequest,
    val sslContext: SslContext?
) : TunnelConnection {
    private val logger by loggerDelegate()

    override val tunnelRequest get() = finalTunnelRequest ?: originalTunnelRequest

    private var openChannelFuture: ChannelFuture? = null

    var finalTunnelRequest: TunnelRequest? = null

    private val activeClosedFlag = AtomicBoolean(false)

    val isActiveClosed get() = activeClosedFlag.get()

    fun open(bootstrap: Bootstrap, failure: (conn: DefaultTunnelConnection) -> Unit) {
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

    fun close() {
        activeClosedFlag.set(true)
        openChannelFuture?.apply {
            channel().attr(AK_TUNNEL_CONNECTION).set(null)
            channel().close()
        }
    }

    override fun toString(): String = tunnelRequest.toString(serverAddr)

}