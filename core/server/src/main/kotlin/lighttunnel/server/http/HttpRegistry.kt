package lighttunnel.server.http

import io.netty.channel.Channel
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

    private val tunnelIdHttpFds = HashMap<Long, HttpFd>()
    private val hostHttpFds = HashMap<String, HttpFd>()
    private val lock = ReentrantReadWriteLock()

    @Throws(ProtoException::class)
    fun register(host: String, sessionChannels: SessionChannels) {
        if (isRegistered(host)) {
            throw ProtoException("host($host) already used")
        }
        val httpFd = HttpFd(host, sessionChannels)
        lock.write {
            tunnelIdHttpFds[sessionChannels.tunnelId] = httpFd
            hostHttpFds[host] = httpFd
        }
        logger.info("Start Tunnel: {}, Options: {}", sessionChannels.tunnelRequest, sessionChannels.tunnelRequest.optionsString)
        logger.trace("hostHttpFds: {}", hostHttpFds)
        logger.trace("tunnelIdHttpFds: {}", tunnelIdHttpFds)
    }

    fun unregister(host: String?) = lock.write {
        unsafeUnregister(host)
        hostHttpFds.remove(host)
        Unit
    }

    fun destroy() = lock.write {
        hostHttpFds.forEach { (host, _) -> unsafeUnregister(host) }
        hostHttpFds.clear()
        Unit
    }

    fun isRegistered(host: String): Boolean = lock.read { hostHttpFds.contains(host) }

    fun getSessionChannel(tunnelId: Long, sessionId: Long): Channel? = lock.read { tunnelIdHttpFds[tunnelId]?.sessionChannels?.getChannel(sessionId) }

    fun getHttpFd(host: String): HttpFd? = lock.read { hostHttpFds[host] }

    val snapshot: JSONArray
        get() = lock.read {
            JSONArray().also { array ->
                tunnelIdHttpFds.values.forEach { fd ->
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
            tunnelIdHttpFds.remove(it.tunnelId)
            it.close()
            logger.info("Shutdown Tunnel: {}", it.tunnelRequest)
        }
    }

}