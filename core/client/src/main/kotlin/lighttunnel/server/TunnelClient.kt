@file:Suppress("unused")

package lighttunnel.server

import io.netty.handler.ssl.SslContext
import lighttunnel.base.TunnelRequest
import lighttunnel.server.conn.TunnelConn
import lighttunnel.server.conn.impl.TunnelConnImpl
import lighttunnel.server.listener.OnRemoteConnectionListener
import lighttunnel.server.listener.OnTunnelConnectionListener
import kotlin.experimental.or

class TunnelClient(
    workerThreads: Int = -1,
    private val retryConnectPolicy: Byte = RETRY_CONNECT_POLICY_LOSE or RETRY_CONNECT_POLICY_ERROR,
    private val onTunnelConnectionListener: OnTunnelConnectionListener? = null,
    private val onRemoteConnectionListener: OnRemoteConnectionListener? = null
) {

    companion object {
        /** 断线重连 */
        const val RETRY_CONNECT_POLICY_LOSE = (1 shl 0).toByte()  // 0000_0001

        /** 错误重连 */
        const val RETRY_CONNECT_POLICY_ERROR = (1 shl 1).toByte() // 0000_0010
    }

    private val daemon by lazy {
        TunnelClientDaemon(
            workerThreads = workerThreads,
            retryConnectPolicy = retryConnectPolicy,
            onTunnelConnectionListener = onTunnelConnectionListener,
            onRemoteConnectionListener = onRemoteConnectionListener
        )
    }

    fun connect(
        serverAddr: String,
        serverPort: Int,
        tunnelRequest: TunnelRequest,
        sslContext: SslContext? = null
    ): TunnelConn = daemon.connect(serverAddr, serverPort, tunnelRequest, sslContext)

    fun close(conn: TunnelConn) = daemon.close((conn as TunnelConnImpl))

    fun getTunnelConnectionList(): List<TunnelConn> = daemon.tunnelConnectionRegistry.tunnelConnectionList

    fun depose() = daemon.depose()

}
