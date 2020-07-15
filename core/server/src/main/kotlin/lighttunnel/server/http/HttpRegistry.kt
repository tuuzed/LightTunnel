@file:Suppress("DuplicatedCode")

package lighttunnel.server.http

import lighttunnel.base.logger.loggerDelegate
import lighttunnel.openapi.ProtoException
import lighttunnel.server.util.SessionChannels
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

internal class HttpRegistry {
    private val logger by loggerDelegate()

    private val hostHttpFds = hashMapOf<String, HttpFdDefaultImpl>()
    private val lock = ReentrantReadWriteLock()

    @Throws(ProtoException::class)
    fun register(isHttps: Boolean, host: String, sessionChannels: SessionChannels): HttpFdDefaultImpl {
        if (isRegistered(host)) {
            throw ProtoException("host($host) already used")
        }
        return HttpFdDefaultImpl(isHttps, sessionChannels).also { fd ->
            lock.write { hostHttpFds[host] = fd }
            logger.debug("Start Tunnel: {}, Options: {}", fd.tunnelRequest, fd.tunnelRequest.extrasString)
        }
    }

    fun unregister(host: String?): HttpFdDefaultImpl? = lock.write {
        unsafeUnregister(host)
        hostHttpFds.remove(host)
    }

    fun depose() = lock.write {
        hostHttpFds.forEach { (host, _) -> unsafeUnregister(host) }
        hostHttpFds.clear()
    }

    fun isRegistered(host: String): Boolean = lock.read { hostHttpFds.contains(host) }

    fun getHttpFd(host: String): HttpFdDefaultImpl? = lock.read { hostHttpFds[host] }

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