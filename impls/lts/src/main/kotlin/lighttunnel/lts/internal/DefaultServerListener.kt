package lighttunnel.lts.internal

import lighttunnel.common.extensions.injectLogger
import lighttunnel.server.ServerListener
import lighttunnel.server.http.HttpDescriptor
import lighttunnel.server.tcp.TcpDescriptor

internal class DefaultServerListener : ServerListener {
    private val logger by injectLogger()

    override fun onTcpTunnelConnected(descriptor: TcpDescriptor) {
        DataStore.tcpDescriptors.add(descriptor)
        logger.info("onConnected: {}", descriptor)
    }

    override fun onTcpTunnelDisconnect(descriptor: TcpDescriptor) {
        DataStore.tcpDescriptors.remove(descriptor)
        logger.info("onDisconnect: {}", descriptor)
    }

    override fun onHttpTunnelConnected(descriptor: HttpDescriptor) {
        (if (descriptor.isHttps) DataStore.httpsDescriptors else DataStore.httpDescriptors).add(descriptor)
        logger.info("onConnected: {}", descriptor)
    }

    override fun onHttpTunnelDisconnect(descriptor: HttpDescriptor) {
        (if (descriptor.isHttps) DataStore.httpsDescriptors else DataStore.httpDescriptors).remove(descriptor)
        logger.info("onDisconnect: {}", descriptor)
    }

}
