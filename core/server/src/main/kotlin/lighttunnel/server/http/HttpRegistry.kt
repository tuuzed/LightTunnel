package lighttunnel.server.http

import io.netty.channel.Channel
import lighttunnel.logger.loggerDelegate
import lighttunnel.proto.ProtoException
import lighttunnel.server.SessionChannels
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap


class HttpRegistry {
    private val logger by loggerDelegate()

    private val tunnelIdDescriptors = ConcurrentHashMap<Long, HttpDescriptor>()
    private val hostDescriptors = ConcurrentHashMap<String, HttpDescriptor>()
    private val lock = Object()

    @Throws(ProtoException::class)
    fun register(host: String, sessionChannels: SessionChannels) = lock {
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

    fun unregister(host: String?) = lock {
        host ?: return
        val descriptor = hostDescriptors.remove(host)
        if (descriptor != null) {
            tunnelIdDescriptors.remove(descriptor.sessionChannels.tunnelId)
            descriptor.close()
            logger.info("Shutdown Tunnel: {}", descriptor.sessionChannels.tunnelRequest)
        }
    }

    fun isRegistered(host: String): Boolean = lock { hostDescriptors.containsKey(host) }

    fun getSessionChannel(tunnelId: Long, sessionId: Long): Channel? = lock {
        val descriptor = tunnelIdDescriptors[tunnelId] ?: return null
        return descriptor.sessionChannels.getChannel(sessionId)
    }

    fun getDescriptor(host: String): HttpDescriptor? = lock { hostDescriptors[host] }

    fun destroy() = lock {
        hostDescriptors.forEach { (host, _) -> unregister(host) }
    }

    val snapshot: JSONArray
        get() = lock {
            val array = JSONArray()
            tunnelIdDescriptors.values.forEach {
                array.put(
                    JSONObject().also { obj ->
                        obj.put("host", it.host)
                        obj.put("connections", it.channelCount)
                        obj.put("local_addr", it.tunnelRequest.localAddr)
                        obj.put("local_port", it.tunnelRequest.localPort)
                    }
                )
            }
            array
        }

    private inline fun <R> lock(block: () -> R): R = synchronized(lock, block)


}