package lighttunnel.server.tcp

import io.netty.channel.Channel
import lighttunnel.logger.loggerDelegate
import lighttunnel.proto.ProtoException
import lighttunnel.server.SessionChannels
import java.util.concurrent.ConcurrentHashMap

class TcpRegistry {
    private val logger by loggerDelegate()

    private val tunnelIdDescriptors = ConcurrentHashMap<Long, TcpDescriptor>()
    private val portDescriptors = ConcurrentHashMap<Int, TcpDescriptor>()

    @Synchronized
    @Throws(ProtoException::class)
    fun register(port: Int, session: SessionChannels, descriptor: TcpDescriptor) {
        tunnelIdDescriptors[session.tunnelId] = descriptor
        portDescriptors[port] = descriptor
        logger.info("Start Tunnel: {}, Options: {}", session.tunnelRequest, session.tunnelRequest.optionsString)
    }

    @Synchronized
    fun unregister(port: Int) {
        val descriptor = portDescriptors.remove(port)
        if (descriptor != null) {
            tunnelIdDescriptors.remove(descriptor.tunnelId)
            descriptor.close()
            logger.info("Shutdown Tunnel: {}", descriptor.sessionChannels.tunnelRequest)
        }
    }

    @Synchronized
    fun isRegistered(port: Int): Boolean = portDescriptors.containsKey(port)

    @Synchronized
    fun getSessionChannel(tunnelId: Long, sessionId: Long): Channel? {
        return tunnelIdDescriptors[tunnelId]?.sessionChannels?.getChannel(sessionId)
    }

    @Synchronized
    fun getDescriptor(port: Int): TcpDescriptor? = portDescriptors[port]

    @Synchronized
    fun destroy() {
        portDescriptors.forEach { (port, _) -> unregister(port) }
    }

}