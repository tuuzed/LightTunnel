package lighttunnel.internal.base.util

import java.util.concurrent.atomic.AtomicLong

class IncIds {
    private val id = AtomicLong(Long.MIN_VALUE)
    val nextId get() = id.incrementAndGet()
}