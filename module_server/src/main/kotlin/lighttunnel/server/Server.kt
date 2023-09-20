@file:Suppress("unused")

package lighttunnel.server

import lighttunnel.server.args.HttpTunnelArgs
import lighttunnel.server.args.HttpsTunnelArgs
import lighttunnel.server.args.TunnelDaemonArgs
import lighttunnel.server.args.TunnelSslDaemonArgs
import lighttunnel.server.http.DefaultHttpDescriptor
import lighttunnel.server.http.HttpDescriptor
import lighttunnel.server.tcp.DefaultTcpDescriptor
import lighttunnel.server.tcp.TcpDescriptor

class Server(
    bossThreads: Int = -1,
    workerThreads: Int = -1,
    tunnelDaemonArgs: TunnelDaemonArgs? = null,
    tunnelSslDaemonArgs: TunnelSslDaemonArgs? = null,
    httpTunnelArgs: HttpTunnelArgs? = null,
    httpsTunnelArgs: HttpsTunnelArgs? = null,
    serverListener: ServerListener? = null,
) {
    private val daemon by lazy {
        ServerTunnelDaemon(
            bossThreads = bossThreads,
            workerThreads = workerThreads,
            tunnelDaemonArgs = tunnelDaemonArgs,
            tunnelSslDaemonArgs = tunnelSslDaemonArgs,
            httpTunnelArgs = httpTunnelArgs,
            httpsTunnelArgs = httpsTunnelArgs,
            serverListener = serverListener,
        )
    }

    val isSupportSsl = tunnelSslDaemonArgs != null
    val isSupportHttp = httpTunnelArgs != null
    val isSupportHttps = httpsTunnelArgs != null

    val httpPort = httpTunnelArgs?.bindPort
    val httpsPort = httpsTunnelArgs?.bindPort

    @Throws(Exception::class)
    fun start(): Unit = daemon.start()
    fun depose(): Unit = daemon.depose()
    fun getTcpDescriptor(port: Int): TcpDescriptor? = daemon.tcpRegistry.getDescriptor(port)
    fun getHttpDescriptor(vhost: String): HttpDescriptor? = daemon.httpRegistry.getDescriptor(vhost)
    fun getHttpsDescriptor(vhost: String): HttpDescriptor? = daemon.httpsRegistry.getDescriptor(vhost)
    fun getTcpDescriptorSnapshot(): List<TcpDescriptor> = daemon.tcpRegistry.getDescriptorSnapshot()
    fun getHttpDescriptorSnapshot(): List<HttpDescriptor> = daemon.httpRegistry.getDescriptorSnapshot()
    fun getHttpsDescriptorSnapshot(): List<HttpDescriptor> = daemon.httpsRegistry.getDescriptorSnapshot()
    fun forceOff(descriptor: TcpDescriptor): TcpDescriptor? = daemon.tcpRegistry.forceOff((descriptor as DefaultTcpDescriptor).port)
    fun forceOff(descriptor: HttpDescriptor): HttpDescriptor? =
        (if (descriptor.isHttps) daemon.httpsRegistry else daemon.httpRegistry).forceOff((descriptor as DefaultHttpDescriptor).vhost)

    fun isTcpRegistered(port: Int) = daemon.tcpRegistry.isRegistered(port)
    fun isHttpRegistered(vhost: String) = daemon.httpRegistry.isRegistered(vhost)
    fun isHttpsRegistered(vhost: String) = daemon.httpsRegistry.isRegistered(vhost)


}
