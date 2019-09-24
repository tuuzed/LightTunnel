package tpclient

import io.netty.bootstrap.Bootstrap
import io.netty.channel.ChannelFuture
import io.netty.channel.ChannelFutureListener
import tpcommon.TPCommand
import tpcommon.TPMassage
import tpcommon.TPRequest
import tpcommon.logger
import java.util.concurrent.atomic.AtomicBoolean

class TPClientDescriptor(
    private val bootstrap: Bootstrap,
    val serverAddr: String,
    val serverPort: Int,
    val tpRequest: TPRequest
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
                future.channel().writeAndFlush(TPMassage(TPCommand.REQUEST, head = tpRequest.toBytes()))
                future.channel().attr(AK_TPC_DESCRIPTOR).set(this)
            } else {
                listener?.onConnectFailure(this)
            }
        })
    }

    fun shutdown() {
        shutdownFlag.set(true)
        connectChannelFuture?.apply {
            channel().attr(AK_TPC_DESCRIPTOR).set(null)
            channel().close()
        }
    }

    override fun toString() = tpRequest.toString()


}