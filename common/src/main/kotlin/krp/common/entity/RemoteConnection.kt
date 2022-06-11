@file:Suppress("unused", "MemberVisibilityCanBePrivate")

package krp.common.entity

import krp.common.exception.KrpException
import krp.common.utils.getOrDefault
import org.json.JSONObject
import java.io.Serializable
import java.net.InetSocketAddress
import java.net.SocketAddress

class RemoteConnection(val address: SocketAddress) : Serializable {

    companion object {
        private const val serialVersionUID = 1L

        @JvmStatic
        @Throws(KrpException::class)
        fun fromJson(jsonStr: String): RemoteConnection {
            val address = try {
                val json = JSONObject(jsonStr)
                InetSocketAddress(
                    json.getString("hostAddress"),
                    json.getOrDefault("port", -1)
                )
            } catch (e: Exception) {
                throw KrpException("解析失败，数据异常", e)
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

    fun asJsonString(): String? = toJson()?.toString()

    override fun toString(): String = address.toString()

}
