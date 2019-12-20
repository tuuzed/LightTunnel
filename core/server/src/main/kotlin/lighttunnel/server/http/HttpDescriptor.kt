package lighttunnel.server.http

import lighttunnel.server.SessionPool

class HttpDescriptor(
    val host: String,
    val sessionPool: SessionPool
) {
    fun close() = sessionPool.destroy()
}
