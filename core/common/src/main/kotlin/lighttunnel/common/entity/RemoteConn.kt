@file:Suppress("MemberVisibilityCanBePrivate", "ConstPropertyName")

package lighttunnel.common.entity

import lighttunnel.common.exception.LightTunnelException
import lighttunnel.common.extensions.JSONObjectOf
import lighttunnel.common.extensions.getOrDefault
import org.json.JSONObject
import java.io.Serializable
import java.net.InetSocketAddress
import java.net.SocketAddress

@JvmInline
value class RemoteConn(val address: SocketAddress) : Serializable {

    companion object {
        private const val serialVersionUID = 1L

        @JvmStatic
        @Throws(LightTunnelException::class)
        fun fromJson(jsonStr: String): RemoteConn {
            val address = try {
                val json = JSONObject(jsonStr)
                InetSocketAddress(
                    json.getOrDefault<String?>("host", null),
                    json.getOrDefault("port", -1)
                )
            } catch (e: Exception) {
                throw LightTunnelException("解析失败，数据异常", e)
            }
            return RemoteConn(address)
        }
    }

    fun toJson(): JSONObject? {
        return if (address is InetSocketAddress) {
            JSONObjectOf(
                "host" to address.address.hostAddress,
                "port" to address.port,
            )
        } else {
            null
        }
    }

    fun toJsonString(): String? = toJson()?.toString()

    override fun toString(): String = address.toString()

}
