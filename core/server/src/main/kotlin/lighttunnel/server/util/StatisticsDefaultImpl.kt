package lighttunnel.server.util

import lighttunnel.openapi.util.Statistics
import java.util.*
import java.util.concurrent.atomic.AtomicLong

internal class StatisticsDefaultImpl : Statistics {
    private val inboundBytesAtomic = AtomicLong(0)
    private val outboundBytesAtomic = AtomicLong(0)

    override val createAt: Date = Date()
    override val updateAt: Date = Date()
    override val inboundBytes get() = inboundBytesAtomic.get()
    override val outboundBytes get() = outboundBytesAtomic.get()

    fun incInboundBytes(bytes: Int) {
        inboundBytesAtomic.addAndGet(bytes.toLong())
        updateAt.time = System.currentTimeMillis()
    }

    fun incOutboundBytes(bytes: Int) {
        outboundBytesAtomic.addAndGet(bytes.toLong())
        updateAt.time = System.currentTimeMillis()
    }

}