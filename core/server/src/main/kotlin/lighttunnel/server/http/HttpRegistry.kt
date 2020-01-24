package lighttunnel.server.http

import io.netty.channel.Channel
import lighttunnel.logger.loggerDelegate
import lighttunnel.proto.ProtoException
import lighttunnel.server.SessionChannels
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write


class HttpRegistry {
    private val logger by loggerDelegate()

    private val tunnelIdDescriptors = ConcurrentHashMap<Long, HttpDescriptor>()
    private val hostDescriptors = ConcurrentHashMap<String, HttpDescriptor>()
    private val lock = ReentrantReadWriteLock()

    @Throws(ProtoException::class)
    fun register(host: String, sessionChannels: SessionChannels) {
        if (isRegistered(host)) {
            throw ProtoException("host($host) already used")
        }
        val descriptor = HttpDescriptor(host, sessionChannels)
        lock.write {
            tunnelIdDescriptors[sessionChannels.tunnelId] = descriptor
            hostDescriptors[host] = descriptor
        }
        logger.info("Start Tunnel: {}, Options: {}", sessionChannels.tunnelRequest, sessionChannels.tunnelRequest.optionsString)
        logger.trace("hostDescriptors: {}", hostDescriptors)
        logger.trace("tunnelIdDescriptors: {}", tunnelIdDescriptors)
    }

    fun unregister(host: String?) {
        lock.write {
            unsafeUnregister(host)
            hostDescriptors.remove(host)
        }
    }

    fun destroy() {
        lock.write {
            hostDescriptors.forEach { (host, _) -> unsafeUnregister(host) }
            hostDescriptors.clear()
        }
    }

    fun isRegistered(host: String): Boolean {
        lock.read {
            return hostDescriptors.contains(host)
        }
    }

    fun getSessionChannel(tunnelId: Long, sessionId: Long): Channel? {
        lock.read {
            return tunnelIdDescriptors[tunnelId]?.sessionChannels?.getChannel(sessionId)
        }
    }

    fun getDescriptor(host: String): HttpDescriptor? {
        lock.read {
            return hostDescriptors[host]
        }
    }

    val snapshot: JSONArray
        get() {
            lock.read {
                val array = JSONArray()
                tunnelIdDescriptors.values.forEach {
                    array.put(JSONObject().also { obj ->
                        obj.put("host", it.host)
                        obj.put("connections", it.channelCount)
                        obj.put("name", it.tunnelRequest.name)
                        obj.put("local_addr", it.tunnelRequest.localAddr)
                        obj.put("local_port", it.tunnelRequest.localPort)
                    })
                }
                return array
            }
        }

    private fun unsafeUnregister(host: String?) {
        host ?: return
        hostDescriptors[host]?.also {
            tunnelIdDescriptors.remove(it.tunnelId)
            it.close()
            logger.info("Shutdown Tunnel: {}", it.tunnelRequest)
        }
    }

}