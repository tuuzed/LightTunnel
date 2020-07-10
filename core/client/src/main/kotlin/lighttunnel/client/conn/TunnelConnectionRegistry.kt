package lighttunnel.client.conn

import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

class TunnelConnectionRegistry {
    private val cached = arrayListOf<TunnelConnection>()
    private val lock = ReentrantReadWriteLock()

    fun register(conn: TunnelConnection) = lock.write { cached.add(conn) }

    fun unregister(conn: TunnelConnection) = lock.write { cached.remove(conn) }

    fun depose() = lock.write {
        cached.forEach { it.close() }
        cached.clear()
    }

    val snapshot: JSONArray
        get() = lock.read {
            JSONArray().also { array ->
                cached.forEach { fd ->
                    val request = fd.finalTunnelRequest ?: fd.originalTunnelRequest
                    array.put(JSONObject().also {
                        it.put("name", request.name)
                        it.put("tunnel", request.toString(fd.serverAddr))
                    })
                }
            }
        }

}