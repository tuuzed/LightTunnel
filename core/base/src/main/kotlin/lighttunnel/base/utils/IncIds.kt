package lighttunnel.base.utils

import java.util.concurrent.atomic.AtomicLong

class IncIds {
    private val id = AtomicLong(0L)
    val nextId get() = incrementAndGetId
    private val incrementAndGetId get() = id.incrementAndGet()
}
