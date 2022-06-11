package krp.krp.conn

import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

internal class TunnelConnRegistry {

    private val cached = arrayListOf<DefaultTunnelConn>()
    private val lock = ReentrantReadWriteLock()

    fun register(conn: DefaultTunnelConn) = lock.write { cached.add(conn) }

    fun unregister(conn: DefaultTunnelConn) = lock.write { cached.remove(conn) }

    fun depose() = lock.write {
        cached.forEach { it.close() }
        cached.clear()
    }

    val tunnelConnectionList: List<DefaultTunnelConn> get() = lock.read { cached }


}
