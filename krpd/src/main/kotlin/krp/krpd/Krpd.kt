@file:Suppress("unused")

package krp.krpd

import krp.krpd.args.HttpTunnelArgs
import krp.krpd.args.HttpsTunnelArgs
import krp.krpd.args.TunnelDaemonArgs
import krp.krpd.args.TunnelSslDaemonArgs
import krp.krpd.http.DefaultHttpFd
import krp.krpd.http.HttpFd
import krp.krpd.tcp.DefaultTcpFd
import krp.krpd.tcp.TcpFd

class Krpd(
    bossThreads: Int = -1,
    workerThreads: Int = -1,
    tunnelDaemonArgs: TunnelDaemonArgs? = null,
    tunnelSslDaemonArgs: TunnelSslDaemonArgs? = null,
    httpTunnelArgs: HttpTunnelArgs? = null,
    httpsTunnelArgs: HttpsTunnelArgs? = null,
    krpdListener: KrpdListener? = null,
) {
    private val daemon by lazy {
        KrpdTunnelDaemon(
            bossThreads = bossThreads,
            workerThreads = workerThreads,
            tunnelDaemonArgs = tunnelDaemonArgs,
            tunnelSslDaemonArgs = tunnelSslDaemonArgs,
            httpTunnelArgs = httpTunnelArgs,
            httpsTunnelArgs = httpsTunnelArgs,
            krpdListener = krpdListener,
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
    fun getTcpFd(port: Int): TcpFd? = daemon.tcpRegistry.getTcpFd(port)
    fun getHttpFd(vhost: String): HttpFd? = daemon.httpRegistry.getHttpFd(vhost)
    fun getHttpsFd(vhost: String): HttpFd? = daemon.httpsRegistry.getHttpFd(vhost)
    fun getTcpFdList(): List<TcpFd> = daemon.tcpRegistry.getTcpFdList()
    fun getHttpFdList(): List<HttpFd> = daemon.httpRegistry.getHttpFdList()
    fun getHttpsFdList(): List<HttpFd> = daemon.httpsRegistry.getHttpFdList()
    fun forceOff(fd: TcpFd): TcpFd? = daemon.tcpRegistry.forceOff((fd as DefaultTcpFd).port)
    fun forceOff(fd: HttpFd): HttpFd? =
        (if (fd.isHttps) daemon.httpsRegistry else daemon.httpRegistry).forceOff((fd as DefaultHttpFd).vhost)

    fun isTcpRegistered(port: Int) = daemon.tcpRegistry.isRegistered(port)
    fun isHttpRegistered(vhost: String) = daemon.httpRegistry.isRegistered(vhost)
    fun isHttpsRegistered(vhost: String) = daemon.httpsRegistry.isRegistered(vhost)


}
