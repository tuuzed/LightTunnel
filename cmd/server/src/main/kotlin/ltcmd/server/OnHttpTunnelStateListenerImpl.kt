package ltcmd.server

import lighttunnel.logger.loggerDelegate
import lighttunnel.server.TunnelServer
import lighttunnel.server.http.HttpFd

class OnHttpTunnelStateListenerImpl : TunnelServer.OnHttpTunnelStateListener {

    private val logger by loggerDelegate()

    override fun onConnected(fd: HttpFd) {
        logger.info("onConnected: {}", fd)
    }

    override fun onDisconnect(fd: HttpFd) {
        logger.info("onDisconnect: {}", fd)
    }
}
