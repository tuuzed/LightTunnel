package lighttunnel.client.connect

import lighttunnel.proto.ProtoException
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.write

class TunnelConnectRegistry {
    private val cached = arrayListOf<TunnelConnectDescriptor>()
    private val lock = ReentrantReadWriteLock()

    @Throws(ProtoException::class)
    fun register(descriptor: TunnelConnectDescriptor) {
        lock.write { cached.add(descriptor) }
    }

    fun unregister(descriptor: TunnelConnectDescriptor) {
        lock.write { cached.remove(descriptor) }
    }

    fun destroy() {
        lock.write {
            cached.forEach { it.close() }
            cached.clear()
        }
    }

    val snapshot: JSONArray
        get() {
            val array = JSONArray()
            cached.forEach { descriptor ->
                val request = descriptor.finallyTunnelRequest ?: descriptor.tunnelRequest
                array.put(JSONObject().also {
                    it.put("name", request.name)
                    it.put("tunnel", request.toString(descriptor.serverAddr))
                })
            }
            return array
        }

}