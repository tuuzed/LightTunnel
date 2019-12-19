package lighttunnel.client

import io.netty.bootstrap.Bootstrap
import io.netty.channel.ChannelFuture
import io.netty.channel.ChannelFutureListener
import lighttunnel.logging.logger
import lighttunnel.proto.LTCommand
import lighttunnel.proto.LTMassage
import lighttunnel.proto.LTRequest
import java.util.concurrent.atomic.AtomicBoolean

class LTConnDescriptor(
    private val bootstrap: Bootstrap,
    val serverAddr: String,
    val serverPort: Int,
    val request: LTRequest
) {
    private val logger by logger()
    private val shutdownFlag = AtomicBoolean(false)
    val isShutdown get() = shutdownFlag.get()
    private var connectChannelFuture: ChannelFuture? = null

    @JvmOverloads
    fun connect(listener: OnConnectFailureListener? = null) {
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
                future.channel().writeAndFlush(LTMassage(LTCommand.REQUEST, head = request.toBytes()))
                future.channel().attr(AK_LT_CONN_DESCRIPTOR).set(this)
            } else {
                listener?.onConnectFailure(this)
            }
        })
    }

    fun shutdown() {
        shutdownFlag.set(true)
        connectChannelFuture?.apply {
            channel().attr(AK_LT_CONN_DESCRIPTOR).set(null)
            channel().close()
        }
    }

    override fun toString() = request.toString()


}