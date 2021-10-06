@file:Suppress("MemberVisibilityCanBePrivate")

package lighttunnel.base

import lighttunnel.base.proto.ProtoException
import lighttunnel.base.proto.emptyBytes
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
        fun fromBytes(bytes: ByteArray): RemoteConnection {
            val address = try {
                val json = JSONObject(String(bytes, Charsets.UTF_8))
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

    fun toBytes(): ByteArray {
        return if (address is InetSocketAddress) {
            JSONObject().also {
                it.put("hostAddress", address.address.hostAddress)
                it.put("port", address.port)
            }.toString().toByteArray(charset = Charsets.UTF_8)
        } else {
            emptyBytes
        }
    }

    override fun toString(): String = address.toString()

}
