package lighttunnel.server.http

import lighttunnel.common.exception.LightTunnelException
import lighttunnel.common.utils.injectLogger
import lighttunnel.server.SessionChannels
import java.util.concurrent.ConcurrentHashMap

internal class HttpRegistry {
    private val logger by injectLogger()

    private val vhostHttpDescriptors = ConcurrentHashMap<String, DefaultHttpDescriptor>()

    @Throws(LightTunnelException::class)
    fun register(isHttps: Boolean, vhost: String, sessionChannels: SessionChannels): DefaultHttpDescriptor {
        if (isRegistered(vhost)) {
            throw LightTunnelException("vhost($vhost) already used")
        }
        return DefaultHttpDescriptor(isHttps, sessionChannels).also { descriptor ->
            vhostHttpDescriptors[vhost] = descriptor
            logger.debug("Start Tunnel: {}, Extras: {}", descriptor.tunnelRequest, descriptor.tunnelRequest.extras)
        }
    }

    fun unregister(vhost: String?): DefaultHttpDescriptor? {
        unsafeUnregister(vhost)
        return vhostHttpDescriptors.remove(vhost)
    }

    fun depose() {
        vhostHttpDescriptors.forEach { (vhost, _) -> unsafeUnregister(vhost) }
        vhostHttpDescriptors.clear()
    }

    fun isRegistered(vhost: String): Boolean = vhostHttpDescriptors.containsKey(vhost)

    fun getHttpDescriptor(vhost: String): DefaultHttpDescriptor? = vhostHttpDescriptors[vhost]

    fun getHttpDescriptorList() = vhostHttpDescriptors.values.toList()

    fun forceOff(vhost: String) = getHttpDescriptor(vhost)?.apply { writeAndFlushForceOffMsg() }

    private fun unsafeUnregister(vhost: String?) {
        vhost ?: return
        vhostHttpDescriptors[vhost]?.also {
            it.close()
            logger.debug("Shutdown Tunnel: {}", it)
        }
    }

}
