package lighttunnel.server.http

import io.netty.channel.Channel
import lighttunnel.logger.loggerDelegate
import lighttunnel.proto.ProtoException
import lighttunnel.server.SessionChannels
import java.util.concurrent.ConcurrentHashMap


class HttpRegistry {
    private val logger by loggerDelegate()

    private val tunnelIdDescriptors = ConcurrentHashMap<Long, HttpDescriptor>()
    private val hostDescriptors = ConcurrentHashMap<String, HttpDescriptor>()

    @Synchronized
    fun isRegistered(host: String): Boolean = hostDescriptors.containsKey(host)

    @Synchronized
    @Throws(ProtoException::class)
    fun register(host: String, session: SessionChannels) {
        if (isRegistered(host)) throw ProtoException("host($host) already used")
        val descriptor = HttpDescriptor(host, session)
        tunnelIdDescriptors[session.tunnelId] = descriptor
        hostDescriptors[host] = descriptor
        logger.info("Start Tunnel: {}, Options: {}", session.request, session.request.optionsString)
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
            logger.info("Shutdown Tunnel: {}", descriptor.sessionPool.request)
        }
    }

    @Synchronized
    fun getSessionChannel(tunnelId: Long, sessionId: Long): Channel? {
        val descriptor = tunnelIdDescriptors[tunnelId] ?: return null
        return descriptor.sessionPool.getChannel(sessionId)
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