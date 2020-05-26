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

class HttpRegistry internal constructor() {
    private val logger by loggerDelegate()

    private val hostHttpFds = hashMapOf<String, HttpFd>()
    private val lock = ReentrantReadWriteLock()
    private val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")

    @Throws(ProtoException::class)
    internal fun register(host: String, sessionChannels: SessionChannels): HttpFd {
        if (isRegistered(host)) {
            throw ProtoException("host($host) already used")
        }
        val fd = HttpFd(host, sessionChannels)
        lock.write { hostHttpFds[host] = fd }
        logger.debug("Start Tunnel: {}, Options: {}", sessionChannels.tunnelRequest, sessionChannels.tunnelRequest.optionsString)
        return fd
    }

    internal fun unregister(host: String?): HttpFd? = lock.write {
        unsafeUnregister(host)
        val fd = hostHttpFds.remove(host)
        fd
    }

    internal fun depose() = lock.write {
        hostHttpFds.forEach { (host, _) -> unsafeUnregister(host) }
        hostHttpFds.clear()
        Unit
    }

    internal fun isRegistered(host: String): Boolean = lock.read { hostHttpFds.contains(host) }

    internal fun getHttpFd(host: String): HttpFd? = lock.read { hostHttpFds[host] }

    fun forcedOffline(host: String) = getHttpFd(host)?.apply { forcedOffline() }

    val snapshot: JSONArray
        get() = lock.read {
            if (hostHttpFds.isEmpty()) {
                EMPTY_JSON_ARRAY
            } else {
                JSONArray().also { array ->
                    hostHttpFds.values.forEach { fd ->
                        array.put(JSONObject().apply {
                            put("host", fd.host)
                            put("conns", fd.sessionChannels.cachedChannelCount)
                            put("name", fd.sessionChannels.tunnelRequest.name)
                            put("local_addr", fd.sessionChannels.tunnelRequest.localAddr)
                            put("local_port", fd.sessionChannels.tunnelRequest.localPort)
                            put("conn_date", sdf.format(fd.sessionChannels.createAt))
                            put("inbound_bytes", fd.sessionChannels.inboundBytes.get())
                            put("outbound_bytes", fd.sessionChannels.outboundBytes.get())
                        })
                    }
                }
            }
        }

    private fun unsafeUnregister(host: String?) {
        host ?: return
        hostHttpFds[host]?.also {
            it.close()
            logger.debug("Shutdown Tunnel: {}", it.sessionChannels.tunnelRequest)
        }
    }

}