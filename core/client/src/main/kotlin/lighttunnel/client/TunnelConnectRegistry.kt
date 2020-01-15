package lighttunnel.client

import lighttunnel.proto.ProtoException
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

class TunnelConnectRegistry {
    private val cachedTunnelConnectDescriptors = ArrayList<TunnelConnectDescriptor>()
    private val lock = ReentrantReadWriteLock()

    @Throws(ProtoException::class)
    fun register(descriptor: TunnelConnectDescriptor) {
        lock.write {
            cachedTunnelConnectDescriptors.add(descriptor)
        }
    }

    fun unregister(descriptor: TunnelConnectDescriptor) {
        lock.write {
            cachedTunnelConnectDescriptors.remove(descriptor)
        }
    }

    fun destroy() {
        lock.write {
            cachedTunnelConnectDescriptors.forEach { it.close() }
            cachedTunnelConnectDescriptors.clear()
        }
    }


    val snapshot: JSONArray
        get() {
            lock.read {
                val array = JSONArray()
                cachedTunnelConnectDescriptors.forEach { descriptor ->
                    val request = descriptor.finallyTunnelRequest ?: descriptor.tunnelRequest
                    array.put(JSONObject().also {
                        it.put("name", request.name)
                        it.put("tunnel", request.toString(descriptor.serverAddr))
                    })
                }
                return array
            }
        }


}