package com.tuuzed.lighttunnel.server

import com.tuuzed.lighttunnel.common.logger
import io.netty.channel.Channel
import java.util.concurrent.ConcurrentHashMap

class LTTcpRegistry {
    private val logger by logger()

    private val tunnelIdDescriptors = ConcurrentHashMap<Long, LTTcpDescriptor>()
    private val portDescriptors = ConcurrentHashMap<Int, LTTcpDescriptor>()

    @Synchronized
    fun register(port: Int, session: LTSessionPool, descriptor: LTTcpDescriptor) {
        tunnelIdDescriptors[session.tunnelId] = descriptor
        portDescriptors[port] = descriptor
        logger.info("Start Tunnel: {}", session.request)
    }

    @Synchronized
    fun unregister(tunnelId: Long) {
        val descriptor = tunnelIdDescriptors.remove(tunnelId)
        if (descriptor != null) {
            portDescriptors.remove(descriptor.port)
            descriptor.close()
            logger.info("Shutdown Tunnel: {}", descriptor.sessionPool.request)
        }
    }

    @Synchronized
    fun getSessionChannel(tunnelId: Long, sessionId: Long): Channel? =
        tunnelIdDescriptors[tunnelId]?.sessionPool?.getChannel(sessionId)

    @Synchronized
    fun getDescriptorByPort(port: Int): LTTcpDescriptor? = portDescriptors[port]

    @Synchronized
    fun destroy() {
        tunnelIdDescriptors.clear()
        portDescriptors.clear()
    }

}