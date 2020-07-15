package lighttunnel.server.tcp

import lighttunnel.server.util.EMPTY_JSON_ARRAY
import lighttunnel.server.util.format
import org.json.JSONArray
import org.json.JSONObject

@Suppress("DuplicatedCode")
internal fun TcpRegistry.toJson(): JSONArray {
    val fds = this.getTcpFdList()
    return when (fds.size) {
        0 -> EMPTY_JSON_ARRAY
        else -> JSONArray(
            fds.map { fd ->
                JSONObject().apply {
                    put("port", fd.port)
                    put("conns", fd.connectionCount)
                    put("name", fd.tunnelRequest.name)
                    put("localAddr", fd.tunnelRequest.localAddr)
                    put("localPort", fd.tunnelRequest.localPort)
                    put("inboundBytes", fd.statistics.inboundBytes)
                    put("outboundBytes", fd.statistics.outboundBytes)
                    put("createAt", fd.statistics.createAt.format())
                    put("updateAt", fd.statistics.updateAt.format())
                }
            }
        )
    }
}