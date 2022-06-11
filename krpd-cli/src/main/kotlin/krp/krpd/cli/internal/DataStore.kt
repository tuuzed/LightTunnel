package krp.krpd.cli.internal

import krp.krpd.http.HttpFd
import krp.krpd.tcp.TcpFd
import java.util.*
import java.util.concurrent.ConcurrentHashMap

internal object DataStore {
    val tcpFds: MutableSet<TcpFd> = Collections.newSetFromMap(ConcurrentHashMap())
    val httpFds: MutableSet<HttpFd> = Collections.newSetFromMap(ConcurrentHashMap())
    val httpsFds: MutableSet<HttpFd> = Collections.newSetFromMap(ConcurrentHashMap())
}
