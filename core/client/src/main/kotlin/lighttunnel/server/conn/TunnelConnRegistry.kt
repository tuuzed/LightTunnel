package lighttunnel.server.conn

import lighttunnel.server.conn.impl.TunnelConnImpl
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

internal class TunnelConnRegistry {

    private val cached = arrayListOf<TunnelConnImpl>()
    private val lock = ReentrantReadWriteLock()

    fun register(conn: TunnelConnImpl) = lock.write { cached.add(conn) }

    fun unregister(conn: TunnelConnImpl) = lock.write { cached.remove(conn) }

    fun depose() = lock.write {
        cached.forEach { it.close() }
        cached.clear()
    }

    val tunnelConnectionList: List<TunnelConnImpl> get() = lock.read { cached }


}
