package lighttunnel.lts.cli.internal

import lighttunnel.server.http.HttpDescriptor
import lighttunnel.server.tcp.TcpDescriptor
import java.util.*
import java.util.concurrent.ConcurrentHashMap

internal object DataStore {
    val tcpDescriptors: MutableSet<TcpDescriptor> = Collections.newSetFromMap(ConcurrentHashMap())
    val httDescriptors: MutableSet<HttpDescriptor> = Collections.newSetFromMap(ConcurrentHashMap())
    val httpsDescriptors: MutableSet<HttpDescriptor> = Collections.newSetFromMap(ConcurrentHashMap())
}
