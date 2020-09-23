package lighttunnel.internal.client.conn

import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

internal class TunnelConnectionRegistry {

    private val cached = arrayListOf<TunnelConnectionDefaultImpl>()
    private val lock = ReentrantReadWriteLock()

    fun register(conn: TunnelConnectionDefaultImpl) = lock.write { cached.add(conn) }

    fun unregister(conn: TunnelConnectionDefaultImpl) = lock.write { cached.remove(conn) }

    fun depose() = lock.write {
        cached.forEach { it.close() }
        cached.clear()
    }

    val tunnelConnectionList: List<TunnelConnectionDefaultImpl> get() = lock.read { cached }


}