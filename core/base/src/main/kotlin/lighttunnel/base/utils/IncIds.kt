package lighttunnel.base.utils

import java.util.concurrent.atomic.AtomicLong

class IncIds {
    private val id = AtomicLong(Long.MIN_VALUE)
    val nextId get() = incrementAndGetId.let { if (it == 0L) incrementAndGetId else it }
    private val incrementAndGetId get() = id.incrementAndGet()
}
