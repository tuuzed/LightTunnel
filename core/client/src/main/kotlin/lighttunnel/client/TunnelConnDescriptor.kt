package lighttunnel.client

import io.netty.bootstrap.Bootstrap
import io.netty.channel.ChannelFuture
import io.netty.channel.ChannelFutureListener
import lighttunnel.client.util.AttrKeys
import lighttunnel.logging.loggerDelegate
import lighttunnel.proto.ProtoCommand
import lighttunnel.proto.ProtoMassage
import lighttunnel.proto.ProtoRequest
import java.util.concurrent.atomic.AtomicBoolean

class TunnelConnDescriptor(
    private val bootstrap: Bootstrap,
    val serverAddr: String,
    val serverPort: Int,
    val request: ProtoRequest
) {
    private val logger by loggerDelegate()
    private val shutdownFlag = AtomicBoolean(false)
    private var connectChannelFuture: ChannelFuture? = null

    val isShutdown get() = shutdownFlag.get()

    @JvmOverloads
    fun connect(listener: OnConnectFailureListener? = null) {
        if (shutdownFlag.get()) {
            logger.warn("This tunnel already shutdown.")
            return
        }
        val f = bootstrap.connect(serverAddr, serverPort).also { connectChannelFuture = it }
        @Suppress("RedundantSamConstructor")
        f.addListener(ChannelFutureListener { future ->
            if (future.isSuccess) {
                // 连接成功，向服务器发送请求建立隧道消息
                future.channel().writeAndFlush(ProtoMassage(ProtoCommand.REQUEST, head = request.toBytes()))
                future.channel().attr(AttrKeys.AK_LT_CONN_DESCRIPTOR).set(this)
            } else {
                listener?.onConnectFailure(this)
            }
        })
    }

    fun shutdown() {
        shutdownFlag.set(true)
        connectChannelFuture?.apply {
            channel().attr(AttrKeys.AK_LT_CONN_DESCRIPTOR).set(null)
            channel().close()
        }
    }

    override fun toString() = request.toString()


}