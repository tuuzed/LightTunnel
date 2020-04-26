@file:Suppress("MemberVisibilityCanBePrivate")

package lighttunnel.client.connect

import io.netty.bootstrap.Bootstrap
import io.netty.channel.ChannelFuture
import io.netty.channel.ChannelFutureListener
import lighttunnel.client.util.AttributeKeys
import lighttunnel.logger.loggerDelegate
import lighttunnel.proto.ProtoMessage
import lighttunnel.proto.ProtoMessageType
import lighttunnel.proto.TunnelRequest
import java.util.concurrent.atomic.AtomicBoolean

class TunnelConnectFd(
    private val bootstrap: Bootstrap,
    val serverAddr: String,
    val serverPort: Int,
    val tunnelRequest: TunnelRequest
) {
    private val logger by loggerDelegate()
    private var connectChannelFuture: ChannelFuture? = null

    var finallyTunnelRequest: TunnelRequest? = null
        internal set

    private val activeClosedFlag = AtomicBoolean(false)
    val isActiveClosed get() = activeClosedFlag.get()

    internal fun connect(callback: OnConnectFailureCallback? = null) {
        if (isActiveClosed) {
            logger.warn("This tunnel already closed.")
            return
        }
        @Suppress("RedundantSamConstructor")
        bootstrap.connect(serverAddr, serverPort)
            .also { connectChannelFuture = it }
            .addListener(ChannelFutureListener { future ->
                if (future.isSuccess) {
                    // 连接成功，向服务器发送请求建立隧道消息
                    val head = (finallyTunnelRequest ?: tunnelRequest).toBytes()
                    future.channel().writeAndFlush(ProtoMessage(ProtoMessageType.REQUEST, head = head))
                    future.channel().attr(AttributeKeys.AK_TUNNEL_CONNECT_FD).set(this)
                } else {
                    callback?.onConnectFailure(this)
                }
            })
    }

    internal fun close() {
        activeClosedFlag.set(true)
        connectChannelFuture?.apply {
            channel().attr(AttributeKeys.AK_TUNNEL_CONNECT_FD).set(null)
            channel().close()
        }
    }

    override fun toString(): String {
        return finallyTunnelRequest?.toString(serverAddr) ?: tunnelRequest.toString(serverAddr)
    }

    interface OnConnectFailureCallback {
        fun onConnectFailure(fd: TunnelConnectFd) {}
    }

}