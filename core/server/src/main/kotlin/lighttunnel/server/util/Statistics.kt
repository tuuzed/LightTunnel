package lighttunnel.server.util

import java.util.*

class Statistics private constructor() {

    @Suppress("ClassName")
    internal companion object `-Companion` {
        fun newInstance() = Statistics()
    }

    val createAt: Date = Date()
    val updateAt: Date = Date()

    @Volatile
    var inboundBytes = 0L
        private set

    @Volatile
    var outboundBytes = 0L
        private set

    internal fun incInboundBytes(bytes: Int) {
        inboundBytes += bytes
        updateAt.time = System.currentTimeMillis()
    }

    internal fun incOutboundBytes(bytes: Int) {
        outboundBytes += bytes
        updateAt.time = System.currentTimeMillis()
    }

}