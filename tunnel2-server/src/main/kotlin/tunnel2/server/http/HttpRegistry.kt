package tunnel2.server.http

import io.netty.channel.Channel
import tunnel2.common.TunnelException
import tunnel2.common.logging.LoggerFactory
import tunnel2.server.internal.ServerSessionChannels
import java.util.concurrent.ConcurrentHashMap

class HttpRegistry {
    companion object {
        private val logger = LoggerFactory.getLogger(HttpRegistry::class.java)
    }

    private val tunnelIdDescriptors = ConcurrentHashMap<Long, HttpDescriptor>()
    private val vhostDescriptors = ConcurrentHashMap<String, HttpDescriptor>()

    @Synchronized
    fun isRegistered(vhost: String): Boolean = vhostDescriptors.containsKey(vhost)

    @Synchronized
    @Throws(TunnelException::class)
    fun register(vhost: String, sessionChannels: ServerSessionChannels) {
        if (isRegistered(vhost)) throw TunnelException("vhost($vhost) already used")
        val descriptor = HttpDescriptor(vhost, sessionChannels)
        tunnelIdDescriptors[sessionChannels.tunnelId] = descriptor
        vhostDescriptors[vhost] = descriptor
        logger.info("Start Tunnel: {}", sessionChannels.tunnelRequest)
        logger.trace("vhostDescriptors: {}", vhostDescriptors)
        logger.trace("tunnelIdDescriptors: {}", tunnelIdDescriptors)
    }

    @Synchronized
    fun unregister(vhost: String?) {
        vhost ?: return
        val descriptor = vhostDescriptors.remove(vhost)
        if (descriptor != null) {
            tunnelIdDescriptors.remove(descriptor.sessionChannels.tunnelId)
            descriptor.close()
            logger.info("Shutdown Tunnel: {}", descriptor.sessionChannels.tunnelRequest)
        }
    }

    @Synchronized
    fun getSessionChannel(tunnelId: Long, sessionId: Long): Channel? {
        val descriptor = tunnelIdDescriptors[tunnelId] ?: return null
        return descriptor.sessionChannels.getSessionChannel(sessionId)
    }

    @Synchronized
    fun getDescriptorByTunnelId(tunnelId: Long): HttpDescriptor? = tunnelIdDescriptors[tunnelId]

    @Synchronized
    fun getDescriptorByVhost(vhost: String): HttpDescriptor? = vhostDescriptors[vhost]

    @Synchronized
    fun destroy() {
        tunnelIdDescriptors.clear()
        vhostDescriptors.clear()
    }

}