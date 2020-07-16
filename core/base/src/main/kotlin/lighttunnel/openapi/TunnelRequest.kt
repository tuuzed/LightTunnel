@file:Suppress("unused", "DuplicatedCode")

package lighttunnel.openapi

import io.netty.buffer.ByteBufUtil
import io.netty.buffer.Unpooled
import lighttunnel.base.proto.PROTO_VERSION
import lighttunnel.base.util.toStringMap
import org.json.JSONObject
import java.io.Serializable

class TunnelRequest private constructor(
    val type: Type,
    val localAddr: String,
    val localPort: Int,
    private val extras: JSONObject
) : Serializable {

    companion object Factory {
        private const val serialVersionUID = 1L
        private val CHARSET = Charsets.UTF_8

        @Throws(ProtoException::class)
        fun fromBytes(bytes: ByteArray): TunnelRequest {
            val buffer = Unpooled.wrappedBuffer(bytes)
            try {
                // proto version
                val protoVersion = buffer.readByte()
                // type
                val type = Type.codeOf(buffer.readByte())
                // localPort
                val localPort = buffer.readInt()
                // localAddr
                val localAddrBytes = ByteArray(buffer.readInt())
                buffer.readBytes(localAddrBytes)
                val localAddr = String(localAddrBytes, CHARSET)
                // remotePort or host
                var remotePort = 0
                var host = ""
                when (type) {
                    Type.TCP -> {
                        remotePort = buffer.readInt()
                    }
                    Type.HTTP, Type.HTTPS -> {
                        val hostBytes = ByteArray(buffer.readInt())
                        buffer.readBytes(hostBytes)
                        host = String(hostBytes, CHARSET)
                    }
                    else -> error("type error")
                }
                // extras
                val extrasBytes = ByteArray(buffer.readInt())
                buffer.readBytes(extrasBytes)
                val extras = JSONObject(String(extrasBytes, CHARSET))
                return TunnelRequest(type, localAddr, localPort, extras).also {
                    it.protoVersion = protoVersion
                    it.remotePort = remotePort
                    it.host = host
                }
            } catch (e: Exception) {
                throw ProtoException("解析失败，数据异常", e)
            } finally {
                buffer.release()
            }
        }

        fun forTcp(
            localAddr: String,
            localPort: Int,
            remotePort: Int,
            vararg extras: Pair<String, String>,
            extFunc: (TunnelRequest.() -> Unit)? = null
        ): TunnelRequest {
            val finalExtras = JSONObject()
            extras.forEach { finalExtras.put(it.first, it.second) }
            return TunnelRequest(
                type = Type.TCP,
                localAddr = localAddr,
                localPort = localPort,
                extras = finalExtras
            ).also {
                it.remotePort = remotePort
                extFunc?.invoke(it)
            }
        }

        fun forHttp(
            https: Boolean = false,
            localAddr: String,
            localPort: Int,
            host: String,
            vararg extras: Pair<String, String>,
            extFunc: (TunnelRequest.() -> Unit)? = null
        ): TunnelRequest {
            val finalExtras = JSONObject()
            extras.forEach { finalExtras.put(it.first, it.second) }
            return TunnelRequest(
                type = if (https) Type.HTTPS else Type.HTTP,
                localAddr = localAddr,
                localPort = localPort,
                extras = finalExtras
            ).also {
                it.host = host
                extFunc?.invoke(it)
            }
        }
    }

    // common
    var protoVersion = PROTO_VERSION
        private set

    // tcp
    var remotePort: Int = 0
        private set

    // http & https
    var host: String = ""
        private set

    // extras
    fun <T> getExtra(key: String): T? {
        return when {
            extras.has(key) -> {
                try {
                    @Suppress("UNCHECKED_CAST")
                    extras.get(key) as T
                } catch (e: Exception) {
                    null
                }
            }
            else -> null
        }
    }

    fun <T> setExtra(key: String, value: T) {
        extras.put(key, value)
    }

    fun removeExtra(key: String) {
        extras.remove(key)
    }

    fun toBytes(): ByteArray {
        val buffer = Unpooled.buffer()
        try {
            // proto version
            buffer.writeByte(PROTO_VERSION.toInt())
            // type
            buffer.writeByte(type.code.toInt())
            // localPort
            buffer.writeInt(localPort)
            // localAddr
            localAddr.toByteArray(CHARSET).also {
                buffer.writeInt(it.size)
                buffer.writeBytes(it)
            }
            // remotePort or host
            when (type) {
                Type.TCP -> buffer.writeInt(remotePort)
                Type.HTTP, Type.HTTPS -> host.toByteArray(CHARSET).also {
                    buffer.writeInt(it.size)
                    buffer.writeBytes(it)
                }
                else -> error("type error")
            }
            // extra
            extras.toString().toByteArray(CHARSET).also {
                buffer.writeInt(it.size)
                buffer.writeBytes(it)
            }
            return ByteBufUtil.getBytes(buffer)
        } finally {
            buffer.release()
        }
    }

    fun getExtras(vararg excludeKeys: String): Map<String, Any?> = extras.toMap().also {
        excludeKeys.forEach { key -> it.remove(key) }
    }

    fun copyTcp(
        localAddr: String = this.localAddr,
        localPort: Int = this.localPort,
        remotePort: Int = this.remotePort,
        vararg extras: Pair<String, String> = this.extras.toStringMap().map { it.key to it.value }.toTypedArray(),
        extFunc: (TunnelRequest.() -> Unit)? = null
    ) = forTcp(
        localAddr = localAddr,
        localPort = localPort,
        remotePort = remotePort,
        extras = *extras
    ).also {
        extFunc?.invoke(it)
    }

    fun copyHttp(
        https: Boolean = this.type == Type.HTTPS,
        localAddr: String = this.localAddr,
        localPort: Int = this.localPort,
        host: String = this.host,
        vararg extras: Pair<String, String> = this.extras.toStringMap().map { it.key to it.value }.toTypedArray(),
        extFunc: (TunnelRequest.() -> Unit)? = null
    ) = forHttp(
        https = https,
        localAddr = localAddr,
        localPort = localPort,
        host = host,
        extras = *extras
    ).also {
        extFunc?.invoke(it)
    }

    override fun toString(): String = toString("::")
    fun toString(serverAddr: String): String {
        return when (type) {
            Type.TCP -> "tcp://$localAddr:$localPort<-tcp://$serverAddr:$remotePort"
            Type.HTTP -> "http://$localAddr:$localPort<-http://$host"
            Type.HTTPS -> "https://$localAddr:$localPort<-https://$host"
            else -> ""
        }
    }

    enum class Type(val code: Byte) {
        UNKNOWN(0x00.toByte()),
        TCP(0x10.toByte()),
        HTTP(0x30.toByte()),
        HTTPS(0x31.toByte());

        companion object {
            @JvmStatic
            fun codeOf(code: Byte) = values().firstOrNull { it.code == code } ?: UNKNOWN
        }

    }

}
