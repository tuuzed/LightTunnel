package tpserver

import io.netty.channel.Channel
import tpcommon.logger
import java.util.concurrent.ConcurrentHashMap

class TPTcpRegistry {
    private val logger by logger()

    private val tunnelIdDescriptors = ConcurrentHashMap<Long, TPTcpDescriptor>()
    private val portDescriptors = ConcurrentHashMap<Int, TPTcpDescriptor>()

    @Synchronized
    fun register(port: Int, session: TPSessionPool, descriptor: TPTcpDescriptor) {
        tunnelIdDescriptors[session.tunnelId] = descriptor
        portDescriptors[port] = descriptor
        logger.info("Start Tunnel: {}", session.tpRequest)
    }

    @Synchronized
    fun unregister(tunnelId: Long) {
        val descriptor = tunnelIdDescriptors.remove(tunnelId)
        if (descriptor != null) {
            portDescriptors.remove(descriptor.port)
            descriptor.close()
            logger.info("Shutdown Tunnel: {}", descriptor.sessionPool.tpRequest)
        }
    }

    @Synchronized
    fun getSessionChannel(tunnelId: Long, sessionId: Long): Channel? =
        tunnelIdDescriptors[tunnelId]?.sessionPool?.getChannel(sessionId)

    @Synchronized
    fun getDescriptorByPort(port: Int): TPTcpDescriptor? = portDescriptors[port]

    @Synchronized
    fun destroy() {
        tunnelIdDescriptors.clear()
        portDescriptors.clear()
    }

}