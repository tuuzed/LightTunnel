package lighttunnel.server

class LTHttpDescriptor(
    val host: String,
    val sessionPool: LTSessionPool
) {
    fun close() {
        sessionPool.destroy()
    }
}
