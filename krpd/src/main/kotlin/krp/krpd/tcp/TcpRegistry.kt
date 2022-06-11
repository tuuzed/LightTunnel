package krp.krpd.tcp

import krp.common.exception.KrpException
import krp.common.utils.injectLogger
import java.util.concurrent.ConcurrentHashMap

internal class TcpRegistry {
    private val logger by injectLogger()

    private val portTcpFds = ConcurrentHashMap<Int, DefaultTcpFd>()

    @Throws(KrpException::class)
    fun register(port: Int, fd: DefaultTcpFd) {
        if (isRegistered(port)) {
            throw KrpException("port($port) already used")
        }
        portTcpFds[port] = fd
        logger.debug("Start Tunnel: {}, Extras", fd.tunnelRequest, fd.tunnelRequest.extras)
    }

    fun unregister(port: Int): DefaultTcpFd? {
        unsafeUnregister(port)
        return portTcpFds.remove(port)?.apply { close() }
    }

    fun depose() {
        portTcpFds.forEach { (port, _) -> unsafeUnregister(port) }
        portTcpFds.clear()
    }

    fun isRegistered(port: Int): Boolean = portTcpFds.containsKey(port)

    fun getTcpFd(port: Int): DefaultTcpFd? = portTcpFds[port]

    fun getTcpFdList() = portTcpFds.values.toList()

    fun forceOff(port: Int) = getTcpFd(port)?.apply { writeAndFlushForceOffMsg() }

    private fun unsafeUnregister(port: Int) {
        portTcpFds[port]?.also {
            it.close()
            logger.debug("Shutdown Tunnel: {}", it)
        }
    }

}
