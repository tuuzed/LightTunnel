package lighttunnel.client.connect

import lighttunnel.proto.ProtoException
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.CopyOnWriteArrayList

class TunnelConnectRegistry {
    private val cachedTunnelConnectDescriptors = CopyOnWriteArrayList<TunnelConnectDescriptor>()

    @Throws(ProtoException::class)
    fun register(descriptor: TunnelConnectDescriptor) {
        cachedTunnelConnectDescriptors.add(descriptor)
    }

    fun unregister(descriptor: TunnelConnectDescriptor) {
        cachedTunnelConnectDescriptors.remove(descriptor)
    }

    fun destroy() {
        cachedTunnelConnectDescriptors.forEach { it.close() }
        cachedTunnelConnectDescriptors.clear()
    }

    val snapshot: JSONArray
        get() {
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