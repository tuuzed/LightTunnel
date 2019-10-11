package com.tuuzed.lighttunnel.server

import com.tuuzed.lighttunnel.common.LTException
import com.tuuzed.lighttunnel.common.logger
import io.netty.channel.Channel
import java.util.concurrent.ConcurrentHashMap


class LTHttpRegistry {
    private val logger by logger()

    private val tunnelIdDescriptors = ConcurrentHashMap<Long, LTHttpDescriptor>()
    private val hostDescriptors = ConcurrentHashMap<String, LTHttpDescriptor>()

    @Synchronized
    fun isRegistered(host: String): Boolean = hostDescriptors.containsKey(host)

    @Synchronized
    @Throws(LTException::class)
    fun register(host: String, sessionDescriptor: LTSessionPool) {
        if (isRegistered(host)) throw LTException("host($host) already used")
        val descriptor = LTHttpDescriptor(host, sessionDescriptor)
        tunnelIdDescriptors[sessionDescriptor.tunnelId] = descriptor
        hostDescriptors[host] = descriptor
        logger.info("Start Tunnel: {}", sessionDescriptor.request)
        logger.trace("hostDescriptors: {}", hostDescriptors)
        logger.trace("tunnelIdDescriptors: {}", tunnelIdDescriptors)
    }

    @Synchronized
    fun unregister(host: String?) {
        host ?: return
        val descriptor = hostDescriptors.remove(host)
        if (descriptor != null) {
            tunnelIdDescriptors.remove(descriptor.sessionPool.tunnelId)
            descriptor.close()
            logger.info("Shutdown Tunnel: {}", descriptor.sessionPool.request)
        }
    }

    @Synchronized
    fun getSessionChannel(tunnelId: Long, sessionId: Long): Channel? {
        val descriptor = tunnelIdDescriptors[tunnelId] ?: return null
        return descriptor.sessionPool.getChannel(sessionId)
    }

    @Synchronized
    fun getDescriptorByTunnelId(tunnelId: Long): LTHttpDescriptor? = tunnelIdDescriptors[tunnelId]

    @Synchronized
    fun getDescriptorByHost(host: String): LTHttpDescriptor? = hostDescriptors[host]

    @Synchronized
    fun destroy() {
        tunnelIdDescriptors.clear()
        hostDescriptors.clear()
    }

}