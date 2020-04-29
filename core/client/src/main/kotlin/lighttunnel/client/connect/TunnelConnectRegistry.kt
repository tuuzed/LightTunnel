package lighttunnel.client.connect

import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

class TunnelConnectRegistry {
    private val cached = arrayListOf<TunnelConnectFd>()
    private val lock = ReentrantReadWriteLock()

    fun register(fd: TunnelConnectFd) = lock.write { cached.add(fd) }

    fun unregister(fd: TunnelConnectFd) = lock.write { cached.remove(fd) }

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