package lighttunnel.server.http

import io.netty.channel.Channel
import lighttunnel.logger.loggerDelegate
import lighttunnel.proto.ProtoException
import lighttunnel.server.SessionChannels
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReentrantReadWriteLock


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
        lock.writeLock().lock()
        try {
            tunnelIdDescriptors[sessionChannels.tunnelId] = descriptor
            hostDescriptors[host] = descriptor
        } finally {
            lock.writeLock().unlock()
        }
        logger.info("Start Tunnel: {}, Options: {}", sessionChannels.tunnelRequest, sessionChannels.tunnelRequest.optionsString)
        logger.trace("hostDescriptors: {}", hostDescriptors)
        logger.trace("tunnelIdDescriptors: {}", tunnelIdDescriptors)
    }

    fun unregister(host: String?) {
        lock.writeLock().lock()
        try {
            unsafeUnregister(host)
            hostDescriptors.remove(host)
        } finally {
            lock.writeLock().unlock()
        }
    }

    fun destroy() {
        lock.writeLock().lock()
        try {
            hostDescriptors.forEach { (host, _) -> unsafeUnregister(host) }
            hostDescriptors.clear()
        } finally {
            lock.writeLock().unlock()
        }
    }

    fun isRegistered(host: String): Boolean {
        lock.readLock().lock()
        try {
            return hostDescriptors.contains(host)
        } finally {
            lock.readLock().unlock();
        }
    }

    fun getSessionChannel(tunnelId: Long, sessionId: Long): Channel? {
        lock.readLock().lock()
        try {
            return tunnelIdDescriptors[tunnelId]?.sessionChannels?.getChannel(sessionId)
        } finally {
            lock.readLock().unlock()
        }
    }

    fun getDescriptor(host: String): HttpDescriptor? {
        lock.readLock().lock()
        try {
            return hostDescriptors[host]
        } finally {
            lock.readLock().unlock()
        }
    }

    val snapshot: JSONArray
        get() {
            lock.readLock().lock()
            try {
                val array = JSONArray()
                tunnelIdDescriptors.values.forEach {
                    array.put(JSONObject().also { obj ->
                        obj.put("host", it.host)
                        obj.put("connections", it.channelCount)
                        obj.put("local_addr", it.tunnelRequest.localAddr)
                        obj.put("local_port", it.tunnelRequest.localPort)
                    })
                }
                return array
            } finally {
                lock.readLock().unlock()
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