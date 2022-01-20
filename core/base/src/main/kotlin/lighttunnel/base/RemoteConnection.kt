@file:Suppress("MemberVisibilityCanBePrivate")

package lighttunnel.base

import lighttunnel.base.proto.ProtoException
import lighttunnel.base.utils.getOrDefault
import org.json.JSONObject
import java.io.Serializable
import java.net.InetSocketAddress
import java.net.SocketAddress

class RemoteConnection(val address: SocketAddress) : Serializable {

    companion object {
        private const val serialVersionUID = 1L

        @JvmStatic
        @Throws(ProtoException::class)
        fun fromJson(jsonStr: String): RemoteConnection {
            val address = try {
                val json = JSONObject(jsonStr)
                InetSocketAddress(
                    json.getString("hostAddress"),
                    json.getOrDefault("port", -1)
                )
            } catch (e: Exception) {
                throw ProtoException("解析失败，数据异常", e)
            }
            return RemoteConnection(address)
        }
    }

    fun toJson(): JSONObject? {
        return if (address is InetSocketAddress) {
            return JSONObject().also {
                it.put("hostAddress", address.address.hostAddress)
                it.put("port", address.port)
            }
        } else {
            null
        }
    }

    fun toJsonString(): String? = toJson()?.toString()

    override fun toString(): String = address.toString()

}
