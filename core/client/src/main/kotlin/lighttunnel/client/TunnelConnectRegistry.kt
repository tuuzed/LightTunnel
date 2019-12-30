package lighttunnel.client

import lighttunnel.logger.loggerDelegate
import lighttunnel.proto.ProtoException
import org.json.JSONArray
import org.json.JSONObject

class TunnelConnectRegistry {
    private val logger by loggerDelegate()
    private val cachedTunnelConnectDescriptors = ArrayList<TunnelConnectDescriptor>()
    private val lock = Object()

    @Synchronized
    @Throws(ProtoException::class)
    fun register(descriptor: TunnelConnectDescriptor) = lock {
        cachedTunnelConnectDescriptors.add(descriptor)
    }

    @Synchronized
    fun unregister(descriptor: TunnelConnectDescriptor) = lock {
        cachedTunnelConnectDescriptors.remove(descriptor)
    }

    @Synchronized
    fun destroy() = lock {
        cachedTunnelConnectDescriptors.forEach { it.close() }
        cachedTunnelConnectDescriptors.clear()
    }


    val snapshot
        get() = lock {
            JSONArray().also { array ->
                cachedTunnelConnectDescriptors.forEach { descriptor ->
                    array.put(
                        JSONObject().also {
                            it.put("tunnel",
                                (descriptor.finallyTunnelRequest?.let { request ->
                                    request
                                } ?: descriptor.tunnelRequest).toString(descriptor.serverAddr)
                            )
                        }
                    )
                }
            }
        }

    private inline fun <R> lock(block: () -> R): R = synchronized(lock, block)


}