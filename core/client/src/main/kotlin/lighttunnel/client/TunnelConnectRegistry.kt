package lighttunnel.client

import lighttunnel.proto.ProtoException
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.locks.ReentrantReadWriteLock

class TunnelConnectRegistry {
    private val cachedTunnelConnectDescriptors = ArrayList<TunnelConnectDescriptor>()
    private val lock = ReentrantReadWriteLock()

    @Throws(ProtoException::class)
    fun register(descriptor: TunnelConnectDescriptor) {
        lock.writeLock().lock()
        try {
            cachedTunnelConnectDescriptors.add(descriptor)
        } finally {
            lock.writeLock().unlock()
        }
    }

    fun unregister(descriptor: TunnelConnectDescriptor) {
        lock.writeLock().lock()
        try {
            cachedTunnelConnectDescriptors.remove(descriptor)
        } finally {
            lock.writeLock().unlock()
        }
    }

    fun destroy() {
        lock.writeLock().lock()
        try {
            cachedTunnelConnectDescriptors.forEach { it.close() }
            cachedTunnelConnectDescriptors.clear()
        } finally {
            lock.writeLock().unlock()
        }
    }


    val snapshot: JSONArray
        get() {
            lock.readLock().lock()
            try {
                val array = JSONArray()
                cachedTunnelConnectDescriptors.forEach { descriptor ->
                    array.put(JSONObject().also {
                        it.put("tunnel",
                            (descriptor.finallyTunnelRequest?.let { request ->
                                request
                            } ?: descriptor.tunnelRequest).toString(descriptor.serverAddr)
                        )
                    })
                }
                return array
            } finally {
                lock.readLock().unlock()
            }
        }


}