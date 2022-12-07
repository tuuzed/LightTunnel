package lighttunnel.server.tcp

import lighttunnel.common.exception.LightTunnelException
import lighttunnel.common.utils.injectLogger
import java.util.concurrent.ConcurrentHashMap

internal class TcpRegistry {
    private val logger by injectLogger()

    private val portTcpDescriptors = ConcurrentHashMap<Int, DefaultTcpDescriptor>()

    @Throws(LightTunnelException::class)
    fun register(port: Int, descriptor: DefaultTcpDescriptor) {
        if (isRegistered(port)) {
            throw LightTunnelException("port($port) already used")
        }
        portTcpDescriptors[port] = descriptor
        logger.debug("Start Tunnel: {}, Extras: {}", descriptor.tunnelRequest, descriptor.tunnelRequest.extras)
    }

    fun unregister(port: Int): DefaultTcpDescriptor? {
        unsafeUnregister(port)
        return portTcpDescriptors.remove(port)?.apply { close() }
    }

    fun depose() {
        portTcpDescriptors.forEach { (port, _) -> unsafeUnregister(port) }
        portTcpDescriptors.clear()
    }

    fun isRegistered(port: Int): Boolean = portTcpDescriptors.containsKey(port)

    fun getTcpDescriptor(port: Int): DefaultTcpDescriptor? = portTcpDescriptors[port]

    fun getTcpDescriptorList() = portTcpDescriptors.values.toList()

    fun forceOff(port: Int) = getTcpDescriptor(port)?.apply { writeAndFlushForceOffMsg() }

    private fun unsafeUnregister(port: Int) {
        portTcpDescriptors[port]?.also {
            it.close()
            logger.debug("Shutdown Tunnel: {}", it)
        }
    }

}
