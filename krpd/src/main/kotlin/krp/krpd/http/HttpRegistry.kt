package krp.krpd.http

import krp.common.exception.KrpException
import krp.common.utils.injectLogger
import krp.krpd.SessionChannels
import java.util.concurrent.ConcurrentHashMap

internal class HttpRegistry {
    private val logger by injectLogger()

    private val vhostHttpFds = ConcurrentHashMap<String, DefaultHttpFd>()

    @Throws(KrpException::class)
    fun register(isHttps: Boolean, vhost: String, sessionChannels: SessionChannels): DefaultHttpFd {
        if (isRegistered(vhost)) {
            throw KrpException("vhost($vhost) already used")
        }
        return DefaultHttpFd(isHttps, sessionChannels).also { fd ->
            vhostHttpFds[vhost] = fd
            logger.debug("Start Tunnel: {}, Extras", fd.tunnelRequest, fd.tunnelRequest.extras)
        }
    }

    fun unregister(vhost: String?): DefaultHttpFd? {
        unsafeUnregister(vhost)
        return vhostHttpFds.remove(vhost)
    }

    fun depose() {
        vhostHttpFds.forEach { (vhost, _) -> unsafeUnregister(vhost) }
        vhostHttpFds.clear()
    }

    fun isRegistered(vhost: String): Boolean = vhostHttpFds.containsKey(vhost)

    fun getHttpFd(vhost: String): DefaultHttpFd? = vhostHttpFds[vhost]

    fun getHttpFdList() = vhostHttpFds.values.toList()

    fun forceOff(vhost: String) = getHttpFd(vhost)?.apply { writeAndFlushForceOffMsg() }

    private fun unsafeUnregister(vhost: String?) {
        vhost ?: return
        vhostHttpFds[vhost]?.also {
            it.close()
            logger.debug("Shutdown Tunnel: {}", it)
        }
    }

}
