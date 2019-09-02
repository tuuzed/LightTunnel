package tunnel2.client

import io.netty.bootstrap.Bootstrap
import io.netty.channel.ChannelFuture
import io.netty.channel.ChannelFutureListener
import tunnel2.client.internal.AK_TUNNEL_CLIENT_DESCRIPTOR
import tunnel2.common.TunnelRequest
import tunnel2.common.logging.LoggerFactory
import tunnel2.common.proto.ProtoCw
import tunnel2.common.proto.ProtoMessage
import java.util.concurrent.atomic.AtomicBoolean


class TunnelClientDescriptor(
    private val bootstrap: Bootstrap,
    val serverAddr: String,
    val serverPort: Int,
    val tunnelRequest: TunnelRequest
) {
    companion object {
        private val logger = LoggerFactory.getLogger(TunnelClientDescriptor::class.java)
    }

    private val shutdownFlag = AtomicBoolean(false)
    private var connectChannelFuture: ChannelFuture? = null

    fun connect(failureCallback: ConnectFailureCallback) {
        if (shutdownFlag.get()) {
            logger.warn("This tunnel already shutdown.")
            return
        }
        val f = bootstrap.connect(serverAddr, serverPort)
        connectChannelFuture = f
        @Suppress("RedundantSamConstructor")
        f.addListener(ChannelFutureListener { future ->
            if (future.isSuccess) {
                // 连接成功，向服务器发送请求建立隧道消息
                future.channel().writeAndFlush(
                    ProtoMessage(
                        ProtoCw.REQUEST,
                        0,
                        0,
                        tunnelRequest.toBytes()
                    )
                )
                future.channel().attr<TunnelClientDescriptor>(AK_TUNNEL_CLIENT_DESCRIPTOR).set(this@TunnelClientDescriptor)
            } else {
                failureCallback.invoke(this@TunnelClientDescriptor)
            }
        })
    }

    fun isShutdown(): Boolean {
        return shutdownFlag.get()
    }

    fun shutdown() {
        connectChannelFuture?.also {
            shutdownFlag.set(true)
            it.channel().attr(AK_TUNNEL_CLIENT_DESCRIPTOR).set(null)
            it.channel().close()
        }
    }

    override fun toString() = tunnelRequest.toString()

    interface ConnectFailureCallback {
        fun invoke(descriptor: TunnelClientDescriptor) {}
    }

}