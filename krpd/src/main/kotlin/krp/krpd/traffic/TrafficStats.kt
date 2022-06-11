package krp.krpd.traffic

import java.util.*
import java.util.concurrent.atomic.AtomicLong

sealed interface TrafficStats {
    val createAt: Date
    val updateAt: Date
    val inboundBytes: Long
    val outboundBytes: Long
}

internal class DefaultTrafficStats : TrafficStats {
    private val inboundBytesAtomic = AtomicLong(0)
    private val outboundBytesAtomic = AtomicLong(0)

    override val createAt: Date = Date()
    override val updateAt: Date = Date()
    override val inboundBytes get() = inboundBytesAtomic.get()
    override val outboundBytes get() = outboundBytesAtomic.get()

    fun addInboundBytes(bytes: Int) {
        inboundBytesAtomic.addAndGet(bytes.toLong())
        updateAt.time = System.currentTimeMillis()
    }

    fun addOutboundBytes(bytes: Int) {
        outboundBytesAtomic.addAndGet(bytes.toLong())
        updateAt.time = System.currentTimeMillis()
    }

}
