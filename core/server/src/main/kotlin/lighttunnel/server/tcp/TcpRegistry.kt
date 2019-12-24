package lighttunnel.server.tcp

import io.netty.channel.Channel
import lighttunnel.logger.loggerDelegate
import lighttunnel.server.SessionChannels
import java.util.concurrent.ConcurrentHashMap

class TcpRegistry {
    private val logger by loggerDelegate()

    private val tunnelIdDescriptors = ConcurrentHashMap<Long, TcpDescriptor>()
    private val portDescriptors = ConcurrentHashMap<Int, TcpDescriptor>()

    @Synchronized
    fun register(port: Int, session: SessionChannels, descriptor: TcpDescriptor) {
        tunnelIdDescriptors[session.tunnelId] = descriptor
        portDescriptors[port] = descriptor
        logger.info("Start Tunnel: {}, Options: {}", session.request, session.request.optionsString)
    }

    @Synchronized
    fun unregister(tunnelId: Long) {
        val descriptor = tunnelIdDescriptors.remove(tunnelId)
        if (descriptor != null) {
            portDescriptors.remove(descriptor.port)
            descriptor.close()
            logger.info("Shutdown Tunnel: {}", descriptor.sessionPool.request)
        }
    }

    @Synchronized
    fun getSessionChannel(tunnelId: Long, sessionId: Long): Channel? =
        tunnelIdDescriptors[tunnelId]?.sessionPool?.getChannel(sessionId)

    @Synchronized
    fun getDescriptorByPort(port: Int): TcpDescriptor? = portDescriptors[port]

    @Synchronized
    fun destroy() {
        tunnelIdDescriptors.clear()
        portDescriptors.clear()
    }

}