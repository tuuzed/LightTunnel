@file:Suppress("unused")

package lighttunnel.openapi

import io.netty.handler.ssl.SslContext
import lighttunnel.client.TunnelClientDaemon
import lighttunnel.client.conn.DefaultTunnelConnection
import lighttunnel.openapi.conn.TunnelConnection
import lighttunnel.openapi.listener.OnRemoteConnectionListener
import lighttunnel.openapi.listener.OnTunnelConnectionListener
import kotlin.experimental.or

class TunnelClient(
    workerThreads: Int = -1,
    private val retryConnectPolicy: Byte = RETRY_CONNECT_POLICY_LOSE or RETRY_CONNECT_POLICY_ERROR,
    private val httpRpcBindAddr: String? = null,
    private val httpRpcBindPort: Int? = null,
    private val onTunnelConnectionListener: OnTunnelConnectionListener? = null,
    private val onRemoteConnectionListener: OnRemoteConnectionListener? = null
) {

    companion object {
        const val RETRY_CONNECT_POLICY_LOSE = 0x01.toByte()  // 0000_0001
        const val RETRY_CONNECT_POLICY_ERROR = 0x02.toByte() // 0000_0010
    }

    private val daemon by lazy {
        TunnelClientDaemon(
            workerThreads = workerThreads,
            retryConnectPolicy = retryConnectPolicy,
            httpRpcBindAddr = httpRpcBindAddr,
            httpRpcBindPort = httpRpcBindPort,
            onTunnelConnectionListener = onTunnelConnectionListener,
            onRemoteConnectionListener = onRemoteConnectionListener
        )
    }

    fun connect(
        serverAddr: String,
        serverPort: Int,
        tunnelRequest: TunnelRequest,
        sslContext: SslContext? = null
    ): TunnelConnection = daemon.connect(serverAddr, serverPort, tunnelRequest, sslContext)

    fun close(conn: TunnelConnection) = daemon.close((conn as DefaultTunnelConnection))

    fun getTunnelConnectionList(): List<TunnelConnection> = daemon.tunnelConnectionRegistry.conns

    fun depose() = daemon.depose()

}