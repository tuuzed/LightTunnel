package lighttunnel.client.conn

import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

internal class TunnelConnectionRegistry {

    private val cached = arrayListOf<DefaultTunnelConnection>()
    private val lock = ReentrantReadWriteLock()

    fun register(conn: DefaultTunnelConnection) = lock.write { cached.add(conn) }

    fun unregister(conn: DefaultTunnelConnection) = lock.write { cached.remove(conn) }

    fun depose() = lock.write {
        cached.forEach { it.close() }
        cached.clear()
    }

    val conns: List<DefaultTunnelConnection> get() = cached

    fun toJson() = lock.read {
        JSONArray(conns.map {
            JSONObject().apply {
                put("name", it.tunnelRequest.name)
                put("conn", it.toString())
            }
        })
    }


}