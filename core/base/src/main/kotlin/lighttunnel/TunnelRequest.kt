@file:Suppress("unused", "DuplicatedCode")

package lighttunnel

import io.netty.buffer.ByteBufUtil
import io.netty.buffer.Unpooled
import lighttunnel.internal.base.proto.PROTO_VERSION
import lighttunnel.internal.base.util.getOrDefault
import org.json.JSONObject
import java.io.Serializable

class TunnelRequest private constructor(
    val tunnelType: TunnelType,
    private val data: JSONObject
) : Serializable {

    companion object Factory {
        private const val serialVersionUID = 1L
        private val CHARSET = Charsets.UTF_8
        private const val LOCAL_ADDR = "LOCAL_ADDR"
        private const val LOCAL_PORT = "LOCAL_PORT"
        private const val REMOTE_PORT = "REMOTE_PORT"
        private const val HOST = "HOST"
        private const val EXTRAS = "EXTRAS"

        @Throws(ProtoException::class)
        fun fromBytes(bytes: ByteArray): TunnelRequest {
            val buffer = Unpooled.wrappedBuffer(bytes)
            try {
                // protoVersion
                val protoVersion = buffer.readByte()
                // tunnelType
                val tunnelType = TunnelType.codeOf(buffer.readByte())
                // data
                val dataBytes = ByteArray(buffer.readInt())
                buffer.readBytes(dataBytes)
                val data = JSONObject(String(dataBytes, CHARSET))
                return TunnelRequest(tunnelType, data).also {
                    it.protoVersion = protoVersion
                }
            } catch (e: Exception) {
                e.printStackTrace()
                throw ProtoException("解析失败，数据异常", e)
            } finally {
                buffer.release()
            }
        }

        fun forTcp(
            localAddr: String,
            localPort: Int,
            remotePort: Int,
            extFunc: (TunnelRequest.() -> Unit)? = null
        ): TunnelRequest {
            return TunnelRequest(
                tunnelType = TunnelType.TCP,
                data = JSONObject()
            ).apply {
                this.protoVersion = PROTO_VERSION
                this.localAddr = localAddr
                this.localPort = localPort
                this.remotePort = remotePort
                this.extras = JSONObject()
                extFunc?.invoke(this)
            }
        }

        fun forHttp(
            https: Boolean = false,
            localAddr: String,
            localPort: Int,
            host: String,
            extFunc: (TunnelRequest.() -> Unit)? = null
        ): TunnelRequest {
            return TunnelRequest(
                tunnelType = if (https) TunnelType.HTTPS else TunnelType.HTTP,
                data = JSONObject()
            ).apply {
                this.protoVersion = PROTO_VERSION
                this.localAddr = localAddr
                this.localPort = localPort
                this.host = host
                this.extras = JSONObject()
                extFunc?.invoke(this)
            }
        }
    }

    var protoVersion = PROTO_VERSION
        private set

    var localAddr: String
        get() = data.getOrDefault(LOCAL_ADDR, "")
        private set(value) = run { data.put(LOCAL_ADDR, value) }

    var localPort: Int
        get() = data.getOrDefault(LOCAL_PORT, 0)
        private set(value) = run { data.put(LOCAL_PORT, value) }

    var remotePort: Int
        get() = data.getOrDefault(REMOTE_PORT, 0)
        private set(value) = run { data.put(REMOTE_PORT, value) }

    var host: String
        get() = data.getOrDefault(HOST, "")
        set(value) = run { data.put(HOST, value) }

    var extras: JSONObject
        get() = data.getOrDefault(EXTRAS, JSONObject())
        private set(value) = run { data.put(EXTRAS, value) }

    fun copyTcp(
        localAddr: String = this.localAddr,
        localPort: Int = this.localPort,
        remotePort: Int = this.remotePort,
        extFunc: (TunnelRequest.() -> Unit)? = null
    ) = TunnelRequest(TunnelType.TCP, this.data).apply {
        this.localAddr = localAddr
        this.localPort = localPort
        this.remotePort = remotePort
        extFunc?.invoke(this)
    }

    fun copyHttp(
        https: Boolean = this.tunnelType == TunnelType.HTTPS,
        localAddr: String = this.localAddr,
        localPort: Int = this.localPort,
        host: String = this.host,
        extFunc: (TunnelRequest.() -> Unit)? = null
    ) = TunnelRequest(if (https) TunnelType.HTTPS else TunnelType.HTTP, this.data).apply {
        this.localAddr = localAddr
        this.localPort = localPort
        this.host = host
        extFunc?.invoke(this)
    }

    fun toBytes(): ByteArray {
        val buffer = Unpooled.buffer()
        try {
            // proto version
            buffer.writeByte(PROTO_VERSION.toInt())
            // type
            buffer.writeByte(tunnelType.code.toInt())
            // data
            data.toString().toByteArray(CHARSET).also {
                buffer.writeInt(it.size)
                buffer.writeBytes(it)
            }
            return ByteBufUtil.getBytes(buffer)
        } finally {
            buffer.release()
        }
    }


    override fun toString(): String = toString("::")
    fun toString(serverAddr: String): String {
        return when (tunnelType) {
            TunnelType.TCP -> "tcp://$localAddr:$localPort<-tcp://$serverAddr:$remotePort"
            TunnelType.HTTP -> "http://$localAddr:$localPort<-http://$host"
            TunnelType.HTTPS -> "https://$localAddr:$localPort<-https://$host"
            else -> ""
        }
    }

    fun toRawString(): String {
        return "TunnelRequest(protoVersion=$protoVersion,tunnelType=$tunnelType, data=$data)"
    }

}
