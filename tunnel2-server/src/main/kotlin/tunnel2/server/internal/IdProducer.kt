package tunnel2.server.internal

class IdProducer {

    @Volatile
    private var id: Long = 0

    val nextId: Long @Synchronized get() = ++id

}
