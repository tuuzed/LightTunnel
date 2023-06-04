package lighttunnel.common.utils

import java.util.concurrent.atomic.AtomicLong

class IncIds {

    private val value = AtomicLong(0L)

    fun nextId(): Long {
        return value.incrementAndGet()
    }

}
