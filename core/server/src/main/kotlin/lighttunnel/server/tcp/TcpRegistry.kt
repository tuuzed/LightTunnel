@file:Suppress("DuplicatedCode")

package lighttunnel.server.tcp

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

internal class TcpRegistry {
    private val logger by loggerDelegate()

    private val portTcpFds = hashMapOf<Int, DefaultTcpFd>()
    private val lock = ReentrantReadWriteLock()
    private val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")

    @Throws(ProtoException::class)
    fun register(port: Int, sessionChannels: SessionChannels, fd: DefaultTcpFd) {
        if (isRegistered(port)) {
            throw ProtoException("port($port) already used")
        }
        lock.write { portTcpFds[port] = fd }
        logger.debug("Start Tunnel: {}, Options: {}", sessionChannels.tunnelRequest, sessionChannels.tunnelRequest.optionsString)
    }

    fun unregister(port: Int): DefaultTcpFd? = lock.write {
        unsafeUnregister(port)
        portTcpFds.remove(port)?.apply { close() }
    }

    fun depose() = lock.write {
        portTcpFds.forEach { (port, _) -> unsafeUnregister(port) }
        portTcpFds.clear()
    }

    fun isRegistered(port: Int): Boolean = lock.read { portTcpFds.contains(port) }

    fun getTcpFd(port: Int): DefaultTcpFd? = lock.read { portTcpFds[port] }

    fun tcpFds() = lock.read { portTcpFds.values.toList() }

    fun forceOff(port: Int) = getTcpFd(port)?.apply { forceOff() }

    fun toJson() = lock.read {
        if (portTcpFds.isEmpty()) {
            EMPTY_JSON_ARRAY
        } else {
            JSONArray(
                portTcpFds.values.map { fd ->
                    JSONObject().apply {
                        put("port", fd.port)
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

    private fun unsafeUnregister(port: Int) {
        portTcpFds[port]?.also {
            it.close()
            logger.debug("Shutdown Tunnel: {}", it.sessionChannels.tunnelRequest)
        }
    }

}