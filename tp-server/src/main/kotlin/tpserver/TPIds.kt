package tpserver

class TPIds {

    @Volatile
    private var id: Long = 0

    val nextId: Long @Synchronized get() = ++id

}