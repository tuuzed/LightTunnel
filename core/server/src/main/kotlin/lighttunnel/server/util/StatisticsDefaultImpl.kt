package lighttunnel.server.util

import lighttunnel.openapi.util.Statistics
import java.util.*

internal class StatisticsDefaultImpl : Statistics {
    override val createAt: Date = Date()
    override val updateAt: Date = Date()

    @Volatile
    override var inboundBytes = 0L
        private set

    @Volatile
    override var outboundBytes = 0L
        private set

    fun incInboundBytes(bytes: Int) {
        inboundBytes += bytes
        updateAt.time = System.currentTimeMillis()
    }

    fun incOutboundBytes(bytes: Int) {
        outboundBytes += bytes
        updateAt.time = System.currentTimeMillis()
    }

}