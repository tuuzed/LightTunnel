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

    private val tunnelIdDescriptors = HashMap<Long, TcpDescriptor>()
    private val portDescriptors = HashMap<Int, TcpDescriptor>()
    private val lock = ReentrantReadWriteLock()

    @Throws(ProtoException::class)
    fun register(port: Int, sessionChannels: SessionChannels, descriptor: TcpDescriptor) {
        if (isRegistered(port)) {
            throw ProtoException("port($port) already used")
        }
        lock.write {
            tunnelIdDescriptors[sessionChannels.tunnelId] = descriptor
            portDescriptors[port] = descriptor
        }
        logger.info("Start Tunnel: {}, Options: {}", sessionChannels.tunnelRequest, sessionChannels.tunnelRequest.optionsString)
    }

    fun unregister(port: Int) {
        lock.write {
            unsafeUnregister(port)
            portDescriptors.remove(port)
        }
    }

    fun destroy() {
        lock.write {
            portDescriptors.forEach { (host, _) -> unsafeUnregister(host) }
            portDescriptors.clear()
        }
        lock.writeLock().lock()
    }


    fun isRegistered(port: Int): Boolean {
        lock.read {
            return portDescriptors.contains(port)
        }
    }

    fun getSessionChannel(tunnelId: Long, sessionId: Long): Channel? {
        lock.read {
            return tunnelIdDescriptors[tunnelId]?.sessionChannels?.getChannel(sessionId)
        }
    }


    fun getDescriptor(port: Int): TcpDescriptor? {
        lock.read {
            return portDescriptors[port]
        }
    }


    val snapshot: JSONArray
        get() {
            lock.read {
                val array = JSONArray()
                tunnelIdDescriptors.values.forEach {
                    array.put(JSONObject().also { obj ->
                        obj.put("port", it.port)
                        obj.put("conns", it.channelCount)
                        obj.put("name", it.tunnelRequest.name)
                        obj.put("local_addr", it.tunnelRequest.localAddr)
                        obj.put("local_port", it.tunnelRequest.localPort)
                    })
                }
                return array
            }
        }

    private fun unsafeUnregister(port: Int) {
        portDescriptors[port]?.also {
            tunnelIdDescriptors.remove(it.tunnelId)
            it.close()
            logger.info("Shutdown Tunnel: {}", it.tunnelRequest)
        }
    }


}