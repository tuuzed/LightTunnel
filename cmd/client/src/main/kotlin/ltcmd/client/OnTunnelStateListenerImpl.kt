package ltcmd.client

import lighttunnel.client.TunnelClient
import lighttunnel.client.conn.TunnelConnection
import lighttunnel.logger.loggerDelegate

class OnTunnelStateListenerImpl : TunnelClient.OnTunnelStateListener {

    private val logger by loggerDelegate()

    override fun onConnecting(conn: TunnelConnection, retryConnect: Boolean) {
        logger.info("onConnecting: {}, retryConnect: {}", conn, retryConnect)
    }

    override fun onConnected(conn: TunnelConnection) {
        logger.info("onConnected: {}", conn)
    }

    override fun onDisconnect(conn: TunnelConnection, cause: Throwable?) {
        logger.info("onDisconnect: {}, cause: {}", conn, cause)
    }

}