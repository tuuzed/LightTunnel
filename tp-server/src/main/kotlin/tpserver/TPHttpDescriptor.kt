package tpserver

class TPHttpDescriptor(
    val host: String,
    val sessionPool: TPSessionPool
) {
    fun close() {
        sessionPool.destroy()
    }
}
