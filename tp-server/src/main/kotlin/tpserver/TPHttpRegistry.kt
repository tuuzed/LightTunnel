package tpserver

import io.netty.channel.Channel
import tpcommon.TPException
import tpcommon.logger
import java.util.concurrent.ConcurrentHashMap


class TPHttpRegistry {
    private val logger by logger()

    private val tunnelIdDescriptors = ConcurrentHashMap<Long, TPHttpDescriptor>()
    private val hostDescriptors = ConcurrentHashMap<String, TPHttpDescriptor>()

    @Synchronized
    fun isRegistered(host: String): Boolean = hostDescriptors.containsKey(host)

    @Synchronized
    @Throws(TPException::class)
    fun register(host: String, sessionDescriptor: TPSessionPool) {
        if (isRegistered(host)) throw TPException("host($host) already used")
        val descriptor = TPHttpDescriptor(host, sessionDescriptor)
        tunnelIdDescriptors[sessionDescriptor.tunnelId] = descriptor
        hostDescriptors[host] = descriptor
        logger.info("Start Tunnel: {}", sessionDescriptor.tpRequest)
        logger.trace("hostDescriptors: {}", hostDescriptors)
        logger.trace("tunnelIdDescriptors: {}", tunnelIdDescriptors)
    }

    @Synchronized
    fun unregister(host: String?) {
        host ?: return
        val descriptor = hostDescriptors.remove(host)
        if (descriptor != null) {
            tunnelIdDescriptors.remove(descriptor.sessionPool.tunnelId)
            descriptor.close()
            logger.info("Shutdown Tunnel: {}", descriptor.sessionPool.tpRequest)
        }
    }

    @Synchronized
    fun getSessionChannel(tunnelId: Long, sessionId: Long): Channel? {
        val descriptor = tunnelIdDescriptors[tunnelId] ?: return null
        return descriptor.sessionPool.getChannel(sessionId)
    }

    @Synchronized
    fun getDescriptorByTunnelId(tunnelId: Long): TPHttpDescriptor? = tunnelIdDescriptors[tunnelId]

    @Synchronized
    fun getDescriptorByHost(host: String): TPHttpDescriptor? = hostDescriptors[host]

    @Synchronized
    fun destroy() {
        tunnelIdDescriptors.clear()
        hostDescriptors.clear()
    }

}