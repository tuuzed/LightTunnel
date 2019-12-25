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
    @Throws(ProtoException::class)
    fun register(host: String, sessionChannels: SessionChannels) {
        if (isRegistered(host)) {
            throw ProtoException("host($host) already used")
        }
        val descriptor = HttpDescriptor(host, sessionChannels)
        tunnelIdDescriptors[sessionChannels.tunnelId] = descriptor
        hostDescriptors[host] = descriptor
        logger.info("Start Tunnel: {}, Options: {}", sessionChannels.tunnelRequest, sessionChannels.tunnelRequest.optionsString)
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
    fun isRegistered(host: String): Boolean = hostDescriptors.containsKey(host)

    @Synchronized
    fun getSessionChannel(tunnelId: Long, sessionId: Long): Channel? {
        val descriptor = tunnelIdDescriptors[tunnelId] ?: return null
        return descriptor.sessionChannels.getChannel(sessionId)
    }

    @Synchronized
    fun getDescriptor(host: String): HttpDescriptor? = hostDescriptors[host]

    @Synchronized
    fun destroy() {
        hostDescriptors.forEach { (host, _) -> unregister(host) }
    }


}