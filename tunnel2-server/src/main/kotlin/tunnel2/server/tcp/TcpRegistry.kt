package tunnel2.server.tcp

import io.netty.channel.Channel
import tunnel2.common.logging.LoggerFactory
import tunnel2.server.internal.ServerSessionChannels
import java.util.concurrent.ConcurrentHashMap

class TcpRegistry {
    companion object {
        private val logger = LoggerFactory.getLogger(TcpRegistry::class.java)
    }

    private val tunnelIdDescriptors = ConcurrentHashMap<Long, TcpDescriptor>()
    private val portDescriptors = ConcurrentHashMap<Int, TcpDescriptor>()

    @Synchronized
    fun register(port: Int, sessionChannels: ServerSessionChannels, descriptor: TcpDescriptor) {
        tunnelIdDescriptors[sessionChannels.tunnelId] = descriptor
        portDescriptors[port] = descriptor
        logger.info("Start Tunnel: {}", sessionChannels.tunnelRequest)
    }

    @Synchronized
    fun unregister(tunnelId: Long) {
        val descriptor = tunnelIdDescriptors.remove(tunnelId)
        if (descriptor != null) {
            portDescriptors.remove(descriptor.port)
            descriptor.close()
            logger.info("Shutdown Tunnel: {}", descriptor.sessionChannels.tunnelRequest)
        }
    }

    @Synchronized
    fun getSessionChannel(tunnelId: Long, sessionId: Long): Channel? =
        tunnelIdDescriptors[tunnelId]?.sessionChannels?.getSessionChannel(sessionId)

    @Synchronized
    fun getDescriptorByPort(port: Int): TcpDescriptor? = portDescriptors[port]

    @Synchronized
    fun destroy() {
        tunnelIdDescriptors.clear()
        portDescriptors.clear()
    }

}