package lighttunnel.client.conn

import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

internal class TunnelConnectionRegistry {

    private val cached = arrayListOf<DefaultTunnelConnectionImpl>()
    private val lock = ReentrantReadWriteLock()

    fun register(conn: DefaultTunnelConnectionImpl) = lock.write { cached.add(conn) }

    fun unregister(conn: DefaultTunnelConnectionImpl) = lock.write { cached.remove(conn) }

    fun depose() = lock.write {
        cached.forEach { it.close() }
        cached.clear()
    }

    val tunnelConnectionList: List<DefaultTunnelConnectionImpl> get() = lock.read { cached }

    fun toJson() = lock.read {
        JSONArray(tunnelConnectionList.map {
            JSONObject().apply {
                put("name", it.tunnelRequest.name)
                put("conn", it.toString())
            }
        })
    }


}