@file:Suppress("unused")

package krp.krp

import io.netty.handler.ssl.SslContext
import krp.common.entity.TunnelRequest
import krp.krp.conn.DefaultTunnelConn
import krp.krp.conn.TunnelConn
import kotlin.experimental.or

class Krp(
    workerThreads: Int = -1,
    private val retryConnectPolicy: Byte = RETRY_CONNECT_POLICY_LOSE or RETRY_CONNECT_POLICY_ERROR,
    private val krpListener: KrpListener? = null,
) {

    companion object {
        /** 断线重连 */
        const val RETRY_CONNECT_POLICY_LOSE = (1 shl 0).toByte()  // 0000_0001

        /** 错误重连 */
        const val RETRY_CONNECT_POLICY_ERROR = (1 shl 1).toByte() // 0000_0010
    }

    private val daemon by lazy {
        KrpTunnelDaemon(
            workerThreads = workerThreads,
            retryConnectPolicy = retryConnectPolicy,
            krpListener = krpListener,
        )
    }

    fun connect(
        serverIp: String,
        serverPort: Int,
        tunnelRequest: TunnelRequest,
        useEncryption: Boolean,
        sslContext: SslContext? = null,
    ): TunnelConn = daemon.connect(serverIp, serverPort, tunnelRequest, useEncryption, sslContext)

    fun close(conn: TunnelConn) = daemon.close((conn as DefaultTunnelConn))

    fun getTunnelConnectionList(): List<TunnelConn> = daemon.tunnelConnectionList

    fun depose() = daemon.depose()

}
