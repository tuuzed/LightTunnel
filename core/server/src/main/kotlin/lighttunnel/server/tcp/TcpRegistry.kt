package lighttunnel.server.tcp

import io.netty.channel.Channel
import lighttunnel.logger.loggerDelegate
import lighttunnel.proto.ProtoException
import lighttunnel.server.SessionChannels
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReentrantReadWriteLock

class TcpRegistry {
    private val logger by loggerDelegate()

    private val tunnelIdDescriptors = ConcurrentHashMap<Long, TcpDescriptor>()
    private val portDescriptors = ConcurrentHashMap<Int, TcpDescriptor>()
    private val lock = ReentrantReadWriteLock()

    @Throws(ProtoException::class)
    fun register(port: Int, sessionChannels: SessionChannels, descriptor: TcpDescriptor) {
        if (isRegistered(port)) {
            throw ProtoException("port($port) already used")
        }
        lock.writeLock().lock()
        try {
            tunnelIdDescriptors[sessionChannels.tunnelId] = descriptor
            portDescriptors[port] = descriptor
            logger.info("Start Tunnel: {}, Options: {}", sessionChannels.tunnelRequest, sessionChannels.tunnelRequest.optionsString)
        } finally {
            lock.writeLock().unlock()
        }
    }

    fun unregister(port: Int) {
        lock.writeLock().lock()
        try {
            unsafeUnregister(port)
            portDescriptors.remove(port)
        } finally {
            lock.writeLock().unlock()
        }
    }

    fun destroy() {
        lock.writeLock().lock()
        try {
            portDescriptors.forEach { (host, _) -> unsafeUnregister(host) }
            portDescriptors.clear()
        } finally {
            lock.writeLock().unlock()
        }
    }


    fun isRegistered(port: Int): Boolean {
        lock.readLock().lock()
        try {
            return portDescriptors.contains(port)
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


    fun getDescriptor(port: Int): TcpDescriptor? {
        lock.readLock().lock()
        try {
            return portDescriptors[port]
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
                        obj.put("port", it.port)
                        obj.put("conns", it.channelCount)
                        obj.put("local_addr", it.tunnelRequest.localAddr)
                        obj.put("local_port", it.tunnelRequest.localPort)
                    })
                }
                return array
            } finally {
                lock.readLock().unlock()
            }
        }

    private fun unsafeUnregister(port: Int) {
        portDescriptors[port]?.also {
            tunnelIdDescriptors.remove(it.tunnelId)
            it.close()
            logger.info("Shutdown Tunnel: {}", it.tunnelRequest)
        }
    }


}