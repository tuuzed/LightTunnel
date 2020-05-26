package lighttunnel.server.tcp

import lighttunnel.logger.loggerDelegate
import lighttunnel.proto.ProtoException
import lighttunnel.server.util.EMPTY_JSON_ARRAY
import lighttunnel.server.util.SessionChannels
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

class TcpRegistry internal constructor() {
    private val logger by loggerDelegate()

    private val portTcpFds = hashMapOf<Int, TcpFd>()
    private val lock = ReentrantReadWriteLock()

    @Throws(ProtoException::class)
    internal fun register(port: Int, sessionChannels: SessionChannels, descriptor: TcpFd) {
        if (isRegistered(port)) {
            throw ProtoException("port($port) already used")
        }
        lock.write { portTcpFds[port] = descriptor }
        logger.debug("Start Tunnel: {}, Options: {}", sessionChannels.tunnelRequest, sessionChannels.tunnelRequest.optionsString)
    }

    internal fun unregister(port: Int): TcpFd? = lock.write {
        unsafeUnregister(port)
        val tcpFd = portTcpFds.remove(port)
        tcpFd?.close()
        tcpFd
    }

    internal fun depose() = lock.write {
        portTcpFds.forEach { (port, _) -> unsafeUnregister(port) }
        portTcpFds.clear()
        Unit
    }

    internal fun isRegistered(port: Int): Boolean = lock.read { portTcpFds.contains(port) }

    internal fun getTcpFd(port: Int): TcpFd? = lock.read { portTcpFds[port] }

    fun forcedOffline(port: Int) = getTcpFd(port)?.apply { forcedOffline() }

    val snapshot: JSONArray
        get() = lock.read {
            if (portTcpFds.isEmpty()) {
                EMPTY_JSON_ARRAY
            } else {
                JSONArray().also { array ->
                    portTcpFds.values.forEach { fd ->
                        array.put(JSONObject().also { obj ->
                            obj.put("port", fd.port)
                            obj.put("conns", fd.cachedChannelCount)
                            obj.put("name", fd.tunnelRequest.name)
                            obj.put("local_addr", fd.tunnelRequest.localAddr)
                            obj.put("local_port", fd.tunnelRequest.localPort)
                            obj.put("date", fd.createAt)
                        })
                    }
                }
            }
        }

    private fun unsafeUnregister(port: Int) {
        portTcpFds[port]?.also {
            it.close()
            logger.debug("Shutdown Tunnel: {}", it.tunnelRequest)
        }
    }

}