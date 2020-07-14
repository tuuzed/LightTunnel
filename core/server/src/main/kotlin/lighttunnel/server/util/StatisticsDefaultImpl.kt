package lighttunnel.server.util

import lighttunnel.server.openapi.util.Statistics
import java.util.*

internal class StatisticsDefaultImpl : Statistics {
    override val createAt: Date = Date()
    override val updateAt: Date = Date()

    @Volatile
    override var inboundBytes = 0L

    @Volatile
    override var outboundBytes = 0L

    fun incInboundBytes(bytes: Int) {
        inboundBytes += bytes
        updateAt.time = System.currentTimeMillis()
    }

    fun incOutboundBytes(bytes: Int) {
        outboundBytes += bytes
        updateAt.time = System.currentTimeMillis()
    }

}