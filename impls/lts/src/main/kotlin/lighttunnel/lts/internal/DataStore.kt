package lighttunnel.lts.internal

import lighttunnel.server.http.HttpDescriptor
import lighttunnel.server.tcp.TcpDescriptor
import java.util.*
import java.util.concurrent.ConcurrentHashMap

internal object DataStore {
    val tcp: MutableSet<TcpDescriptor> = Collections.newSetFromMap(ConcurrentHashMap())
    val http: MutableSet<HttpDescriptor> = Collections.newSetFromMap(ConcurrentHashMap())
    val https: MutableSet<HttpDescriptor> = Collections.newSetFromMap(ConcurrentHashMap())
}
