@file:Suppress("unused", "DuplicatedCode")

package lighttunnel.openapi

import io.netty.buffer.ByteBufUtil
import io.netty.buffer.Unpooled
import lighttunnel.base.util.toStringMap
import org.json.JSONObject
import java.io.Serializable

class TunnelRequest private constructor(
    val type: Type,
    val localAddr: String,
    val localPort: Int,
    private val extras: JSONObject
) : Serializable {

    companion object {
        private const val serialVersionUID = 1L

        // 协议版本
        private const val PROTO_VERSION: Int = 1
        private val CHARSET = Charsets.UTF_8

        @Throws(ProtoException::class)
        fun fromBytes(bytes: ByteArray): TunnelRequest {
            val buffer = Unpooled.wrappedBuffer(bytes)
            try {
                val type = Type.codeOf(buffer.readByte())
                val localPort = buffer.readInt()
                val localAddrBytes = ByteArray(buffer.readInt())
                buffer.readBytes(localAddrBytes)
                val localAddr = String(localAddrBytes, CHARSET)
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

    // tcp
    val protoVersion get() = PROTO_VERSION

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
            buffer.writeByte(PROTO_VERSION)
            buffer.writeByte(type.code.toInt())
            buffer.writeInt(localPort)
            localAddr.toByteArray(CHARSET).also {
                buffer.writeInt(it.size)
                buffer.writeBytes(it)
            }
            when (type) {
                Type.TCP -> buffer.writeInt(remotePort)
                Type.HTTP, Type.HTTPS -> host.toByteArray(CHARSET).also {
                    buffer.writeInt(it.size)
                    buffer.writeBytes(it)
                }
                else -> error("type error")
            }
            extras.toString().toByteArray(CHARSET).also {
                buffer.writeInt(it.size)
                buffer.writeBytes(it)
            }
            return ByteBufUtil.getBytes(buffer)
        } finally {
            buffer.release()
        }
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

    override fun toString(): String {
        return toString("::")
    }

    val extrasString get() = extras.toString()

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
