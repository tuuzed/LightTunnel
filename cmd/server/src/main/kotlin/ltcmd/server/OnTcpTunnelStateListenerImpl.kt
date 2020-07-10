package ltcmd.server

import lighttunnel.logger.loggerDelegate
import lighttunnel.server.TunnelServer
import lighttunnel.server.tcp.TcpFd

class OnTcpTunnelStateListenerImpl : TunnelServer.OnTcpTunnelStateListener {

    private val logger by loggerDelegate()

    override fun onConnected(fd: TcpFd) {
        logger.info("onConnected: {}", fd)
    }

    override fun onDisconnect(fd: TcpFd) {
        logger.info("onDisconnect: {}", fd)
    }
}