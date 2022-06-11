package krp.krpd.cli.internal

import krp.common.utils.injectLogger
import krp.krpd.KrpdListener
import krp.krpd.http.HttpFd
import krp.krpd.tcp.TcpFd

internal class DefaultKrpdListener : KrpdListener {
    private val logger by injectLogger()

    override fun onTcpTunnelConnected(fd: TcpFd) {
        DataStore.tcpFds.add(fd)
        logger.info("onConnected: {}", fd)
    }

    override fun onTcpTunnelDisconnect(fd: TcpFd) {
        DataStore.tcpFds.remove(fd)
        logger.info("onDisconnect: {}", fd)
    }

    override fun onHttpTunnelConnected(fd: HttpFd) {
        (if (fd.isHttps) DataStore.httpsFds else DataStore.httpFds).add(fd)
        logger.info("onConnected: {}", fd)
    }

    override fun onHttpTunnelDisconnect(fd: HttpFd) {
        (if (fd.isHttps) DataStore.httpsFds else DataStore.httpFds).remove(fd)
        logger.info("onDisconnect: {}", fd)
    }

}
