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
    private val hostDescriptors = ConcurrentHashMap<String, HttpDescriptor>()

    @Synchronized
    fun isRegistered(host: String): Boolean = hostDescriptors.containsKey(host)

    @Synchronized
    @Throws(TunnelException::class)
    fun register(host: String, sessionChannels: ServerSessionChannels) {
        if (isRegistered(host)) throw TunnelException("host($host) already used")
        val descriptor = HttpDescriptor(host, sessionChannels)
        tunnelIdDescriptors[sessionChannels.tunnelId] = descriptor
        hostDescriptors[host] = descriptor
        logger.info("Start Tunnel: {}", sessionChannels.tunnelRequest)
        logger.trace("hostDescriptors: {}", hostDescriptors)
        logger.trace("tunnelIdDescriptors: {}", tunnelIdDescriptors)
    }

    @Synchronized
    fun unregister(host: String?) {
        host ?: return
        val descriptor = hostDescriptors.remove(host)
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
    fun getDescriptorByHost(host: String): HttpDescriptor? = hostDescriptors[host]

    @Synchronized
    fun destroy() {
        tunnelIdDescriptors.clear()
        hostDescriptors.clear()
    }

}