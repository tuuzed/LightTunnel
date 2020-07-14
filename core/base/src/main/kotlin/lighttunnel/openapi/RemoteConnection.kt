package lighttunnel.openapi

import lighttunnel.base.proto.ProtoConsts
import lighttunnel.base.util.getOrDefault
import org.json.JSONObject
import java.io.Serializable
import java.net.InetSocketAddress
import java.net.SocketAddress

data class RemoteConnection constructor(
    val address: SocketAddress
) : Serializable {

    companion object {

        private const val serialVersionUID = 1L

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
                it.put("port", address.port)
                it.put("hostAddress", address.address.hostAddress)
            }.toString().toByteArray(charset = Charsets.UTF_8)
        } else {
            ProtoConsts.emptyBytes
        }
    }

    override fun toString(): String {
        return address.toString()
    }

}