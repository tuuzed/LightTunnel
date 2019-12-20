package lighttunnel.server.util

import java.util.concurrent.atomic.AtomicLong

class IncId {
    private val id = AtomicLong(0)
    val nextId get() = id.incrementAndGet()
}