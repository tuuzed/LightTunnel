@file:Suppress("DuplicatedCode")

package lighttunnel.server.http

import lighttunnel.logger.loggerDelegate
import lighttunnel.proto.ProtoException
import lighttunnel.server.util.EMPTY_JSON_ARRAY
import lighttunnel.server.util.SessionChannels
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

internal class HttpRegistry {
    private val logger by loggerDelegate()

    private val hostHttpFds = hashMapOf<String, HttpFd>()
    private val lock = ReentrantReadWriteLock()
    private val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")

    @Throws(ProtoException::class)
    fun register(isHttps: Boolean, host: String, sessionChannels: SessionChannels): HttpFd {
        if (isRegistered(host)) {
            throw ProtoException("host($host) already used")
        }
        return HttpFd.newInstance(isHttps, host, sessionChannels).also { fd ->
            lock.write { hostHttpFds[host] = fd }
            logger.debug("Start Tunnel: {}, Options: {}", fd.tunnelRequest, fd.tunnelRequest.optionsString)
        }
    }

    fun unregister(host: String?): HttpFd? = lock.write {
        unsafeUnregister(host)
        hostHttpFds.remove(host)
    }

    fun depose() = lock.write {
        hostHttpFds.forEach { (host, _) -> unsafeUnregister(host) }
        hostHttpFds.clear()
    }

    fun isRegistered(host: String): Boolean = lock.read { hostHttpFds.contains(host) }

    fun getHttpFd(host: String): HttpFd? = lock.read { hostHttpFds[host] }

    fun httpFds() = lock.read { hostHttpFds.values.toList() }

    fun forceOff(host: String) = getHttpFd(host)?.apply { forceOff() }

    fun toJson(): JSONArray = lock.read {
        if (hostHttpFds.isEmpty()) {
            EMPTY_JSON_ARRAY
        } else {
            JSONArray(
                hostHttpFds.values.map { fd ->
                    JSONObject().apply {
                        put("host", fd.host)
                        put("conns", fd.connectionCount)
                        put("name", fd.tunnelRequest.name)
                        put("localAddr", fd.tunnelRequest.localAddr)
                        put("localPort", fd.tunnelRequest.localPort)
                        put("inboundBytes", fd.statistics.inboundBytes)
                        put("outboundBytes", fd.statistics.outboundBytes)
                        put("createAt", sdf.format(fd.statistics.createAt))
                        put("updateAt", sdf.format(fd.statistics.updateAt))
                    }
                }
            )
        }
    }

    private fun unsafeUnregister(host: String?) {
        host ?: return
        hostHttpFds[host]?.apply {
            close()
            logger.debug("Shutdown Tunnel: {}", sessionChannels.tunnelRequest)
        }
    }

}