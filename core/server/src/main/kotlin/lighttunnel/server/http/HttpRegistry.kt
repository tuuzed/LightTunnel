@file:Suppress("DuplicatedCode")

package lighttunnel.server.http

import lighttunnel.base.proto.ProtoException
import lighttunnel.base.utils.loggerDelegate
import lighttunnel.server.http.impl.HttpFdImpl
import lighttunnel.server.utils.SessionChannels
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

internal class HttpRegistry {
    private val logger by loggerDelegate()

    private val hostHttpFds = hashMapOf<String, HttpFdImpl>()
    private val lock = ReentrantReadWriteLock()

    @Throws(ProtoException::class)
    fun register(isHttps: Boolean, host: String, sessionChannels: SessionChannels): HttpFdImpl {
        if (isRegistered(host)) {
            throw ProtoException("host($host) already used")
        }
        return HttpFdImpl(isHttps, sessionChannels).also { fd ->
            lock.write { hostHttpFds[host] = fd }
            logger.debug("Start Tunnel: {}, Extras", fd.tunnelRequest, fd.tunnelRequest.extras)
        }
    }

    fun unregister(host: String?): HttpFdImpl? = lock.write {
        unsafeUnregister(host)
        hostHttpFds.remove(host)
    }

    fun depose() = lock.write {
        hostHttpFds.forEach { (host, _) -> unsafeUnregister(host) }
        hostHttpFds.clear()
    }

    fun isRegistered(host: String): Boolean = lock.read { hostHttpFds.contains(host) }

    fun getHttpFd(host: String): HttpFdImpl? = lock.read { hostHttpFds[host] }

    fun getHttpFdList() = lock.read { hostHttpFds.values.toList() }

    fun forceOff(host: String) = getHttpFd(host)?.apply { forceOff() }

    private fun unsafeUnregister(host: String?) {
        host ?: return
        hostHttpFds[host]?.apply {
            close()
            logger.debug("Shutdown Tunnel: {}", sessionChannels.tunnelRequest)
        }
    }

}
