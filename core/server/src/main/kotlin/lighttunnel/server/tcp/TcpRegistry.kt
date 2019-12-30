package lighttunnel.server.tcp

import io.netty.channel.Channel
import lighttunnel.logger.loggerDelegate
import lighttunnel.proto.ProtoException
import lighttunnel.server.SessionChannels
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap

class TcpRegistry {
    private val logger by loggerDelegate()

    private val tunnelIdDescriptors = ConcurrentHashMap<Long, TcpDescriptor>()
    private val portDescriptors = ConcurrentHashMap<Int, TcpDescriptor>()
    private val lock = Object()

    @Throws(ProtoException::class)
    fun register(port: Int, sessionChannels: SessionChannels, descriptor: TcpDescriptor) = lock {
        tunnelIdDescriptors[sessionChannels.tunnelId] = descriptor
        portDescriptors[port] = descriptor
        logger.info("Start Tunnel: {}, Options: {}", sessionChannels.tunnelRequest, sessionChannels.tunnelRequest.optionsString)
    }

    fun unregister(port: Int) = lock {
        val descriptor = portDescriptors.remove(port)
        if (descriptor != null) {
            tunnelIdDescriptors.remove(descriptor.tunnelId)
            descriptor.close()
            logger.info("Shutdown Tunnel: {}", descriptor.sessionChannels.tunnelRequest)
        }
    }

    fun isRegistered(port: Int): Boolean = lock {
        portDescriptors.containsKey(port)
    }

    fun getSessionChannel(tunnelId: Long, sessionId: Long): Channel? = lock {
        tunnelIdDescriptors[tunnelId]?.sessionChannels?.getChannel(sessionId)
    }

    fun getDescriptor(port: Int): TcpDescriptor? = lock { portDescriptors[port] }

    fun destroy() = lock {
        portDescriptors.forEach { (port, _) -> unregister(port) }
    }

    val snapshot: JSONArray
        get() = lock {
            val array = JSONArray()
            tunnelIdDescriptors.values.forEach {
                array.put(
                    JSONObject().also { obj ->
                        obj.put("port", it.port)
                        obj.put("conns", it.channelCount)
                        obj.put("local_addr", it.tunnelRequest.localAddr)
                        obj.put("local_port", it.tunnelRequest.localPort)
                    }
                )
            }
            array
        }

    private inline fun <R> lock(block: () -> R): R = synchronized(lock, block)


}