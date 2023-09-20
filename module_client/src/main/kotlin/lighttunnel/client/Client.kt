@file:Suppress("unused")

package lighttunnel.client

import io.netty.handler.ssl.SslContext
import lighttunnel.client.conn.DefaultTunnelConn
import lighttunnel.client.conn.TunnelConn
import lighttunnel.common.entity.TunnelRequest
import kotlin.experimental.or

class Client(
    workerThreads: Int = -1,
    private val retryConnectPolicy: Byte = RETRY_CONNECT_POLICY_LOSE or RETRY_CONNECT_POLICY_ERROR,
    private val clientListener: ClientListener? = null,
) {

    companion object {
        /** 断线重连 */
        const val RETRY_CONNECT_POLICY_LOSE = (1 shl 0).toByte()  // 0000_0001

        /** 错误重连 */
        const val RETRY_CONNECT_POLICY_ERROR = (1 shl 1).toByte() // 0000_0010
    }

    private val daemon by lazy {
        ClientTunnelDaemon(
            workerThreads = workerThreads,
            retryConnectPolicy = retryConnectPolicy,
            clientListener = clientListener,
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

    fun getTunnelConnectionList(): List<TunnelConn> = daemon.tunnelConnectionSnapshot

    fun depose() = daemon.depose()

}
