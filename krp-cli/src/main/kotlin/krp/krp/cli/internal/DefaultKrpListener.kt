package krp.krp.cli.internal

import krp.common.entity.RemoteConnection
import krp.common.exception.KrpException
import krp.common.utils.injectLogger
import krp.krp.KrpListener
import krp.krp.conn.TunnelConn

internal class DefaultKrpListener : KrpListener {

    private val logger by injectLogger()

    override fun onRemoteConnected(conn: RemoteConnection) = logger.info("onRemoteConnected: {}", conn)
    override fun onRemoteDisconnect(conn: RemoteConnection) = logger.info("onRemoteDisconnect: {}", conn)
    override fun onTunnelConnecting(conn: TunnelConn, retryConnect: Boolean) =
        logger.info("onTunnelConnecting: {}, retryConnect: {}", conn, retryConnect)

    override fun onTunnelConnected(conn: TunnelConn) = logger.info("onTunnelConnected: {}", conn)
    override fun onTunnelDisconnect(conn: TunnelConn, cause: KrpException?) =
        logger.info("onTunnelDisconnect: {}, cause: {}", conn, cause)

}
