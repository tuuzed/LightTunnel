package lighttunnel.ltc.internal

import lighttunnel.client.ClientListener
import lighttunnel.client.conn.TunnelConn
import lighttunnel.common.entity.RemoteConn
import lighttunnel.common.exception.LightTunnelException
import lighttunnel.common.utils.injectLogger

internal class DefaultClientListener : ClientListener {

    private val logger by injectLogger()

    override fun onRemoteConnected(conn: RemoteConn) =
        logger.info("onRemoteConnected: {}", conn)

    override fun onRemoteDisconnect(conn: RemoteConn) =
        logger.info("onRemoteDisconnect: {}", conn)

    override fun onTunnelConnecting(
        conn: TunnelConn, retryConnect: Boolean
    ) = logger.info("onTunnelConnecting: {}, retryConnect: {}", conn, retryConnect)

    override fun onTunnelConnected(conn: TunnelConn) = logger.info("onTunnelConnected: {}", conn)
    override fun onTunnelDisconnect(
        conn: TunnelConn, cause: LightTunnelException?
    ) = logger.info("onTunnelDisconnect: {}, cause: {}", conn, cause ?: "null")

}
