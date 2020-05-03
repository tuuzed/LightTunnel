package lighttunnel.server.http

import lighttunnel.logger.loggerDelegate
import lighttunnel.proto.ProtoException
import lighttunnel.server.util.SessionChannels
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

class HttpRegistry {
    private val logger by loggerDelegate()

    private val hostHttpFds = hashMapOf<String, HttpFd>()
    private val lock = ReentrantReadWriteLock()

    @Throws(ProtoException::class)
    fun register(host: String, sessionChannels: SessionChannels): HttpFd {
        if (isRegistered(host)) {
            throw ProtoException("host($host) already used")
        }
        val fd = HttpFd(host, sessionChannels)
        lock.write { hostHttpFds[host] = fd }
        logger.debug("Start Tunnel: {}, Options: {}", sessionChannels.tunnelRequest, sessionChannels.tunnelRequest.optionsString)
        return fd
    }

    fun unregister(host: String?): HttpFd? = lock.write {
        unsafeUnregister(host)
        val fd = hostHttpFds.remove(host)
        fd
    }

    fun depose() = lock.write {
        hostHttpFds.forEach { (host, _) -> unsafeUnregister(host) }
        hostHttpFds.clear()
        Unit
    }

    fun isRegistered(host: String): Boolean = lock.read { hostHttpFds.contains(host) }

    fun getHttpFd(host: String): HttpFd? = lock.read { hostHttpFds[host] }

    val snapshot: JSONArray
        get() = lock.read {
            JSONArray().also { array ->
                hostHttpFds.values.forEach { fd ->
                    array.put(JSONObject().also { obj ->
                        obj.put("host", fd.host)
                        obj.put("conns", fd.channelCount)
                        obj.put("name", fd.tunnelRequest.name)
                        obj.put("local_addr", fd.tunnelRequest.localAddr)
                        obj.put("local_port", fd.tunnelRequest.localPort)
                    })
                }
            }
        }

    private fun unsafeUnregister(host: String?) {
        host ?: return
        hostHttpFds[host]?.also {
            it.close()
            logger.debug("Shutdown Tunnel: {}", it.tunnelRequest)
        }
    }

}