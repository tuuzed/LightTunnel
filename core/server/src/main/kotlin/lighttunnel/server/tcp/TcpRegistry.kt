package lighttunnel.server.tcp

import io.netty.channel.Channel
import lighttunnel.logger.loggerDelegate
import lighttunnel.proto.ProtoException
import lighttunnel.server.util.SessionChannels
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

class TcpRegistry {
    private val logger by loggerDelegate()

    private val tunnelIdTcpFds = hashMapOf<Long, TcpFd>()
    private val portTcpFds = hashMapOf<Int, TcpFd>()
    private val lock = ReentrantReadWriteLock()

    @Throws(ProtoException::class)
    fun register(port: Int, sessionChannels: SessionChannels, descriptor: TcpFd) {
        if (isRegistered(port)) {
            throw ProtoException("port($port) already used")
        }
        lock.write {
            tunnelIdTcpFds[sessionChannels.tunnelId] = descriptor
            portTcpFds[port] = descriptor
        }
        logger.info("Start Tunnel: {}, Options: {}", sessionChannels.tunnelRequest, sessionChannels.tunnelRequest.optionsString)
    }

    fun unregister(port: Int) = lock.write {
        unsafeUnregister(port)
        portTcpFds.remove(port)
        Unit
    }

    fun depose() = lock.write {
        portTcpFds.forEach { (host, _) -> unsafeUnregister(host) }
        portTcpFds.clear()
        Unit
    }

    fun isRegistered(port: Int): Boolean = lock.read { portTcpFds.contains(port) }

    fun getSessionChannel(tunnelId: Long, sessionId: Long): Channel? = lock.read { tunnelIdTcpFds[tunnelId]?.sessionChannels?.getChannel(sessionId) }

    fun getTcpFd(port: Int): TcpFd? = lock.read { portTcpFds[port] }

    val snapshot: JSONArray
        get() = lock.read {
            JSONArray().also { array ->
                tunnelIdTcpFds.values.forEach {
                    array.put(JSONObject().also { obj ->
                        obj.put("port", it.port)
                        obj.put("conns", it.channelCount)
                        obj.put("name", it.tunnelRequest.name)
                        obj.put("local_addr", it.tunnelRequest.localAddr)
                        obj.put("local_port", it.tunnelRequest.localPort)
                    })
                }
            }
        }

    private fun unsafeUnregister(port: Int) {
        portTcpFds[port]?.also {
            tunnelIdTcpFds.remove(it.tunnelId)
            it.close()
            logger.info("Shutdown Tunnel: {}", it.tunnelRequest)
        }
    }

}