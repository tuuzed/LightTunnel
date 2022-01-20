@file:Suppress("unused", "DuplicatedCode")

package lighttunnel.base

import lighttunnel.base.proto.Proto
import lighttunnel.base.proto.ProtoException
import lighttunnel.base.utils.getOrDefault
import org.json.JSONObject
import java.io.Serializable

class TunnelRequest private constructor(
    private val root: JSONObject
) : Serializable {

    companion object {
        private const val serialVersionUID = 1L
        private const val TUNNEL_TYPE = "TUNNEL_TYPE"
        private const val LOCAL_ADDR = "LOCAL_ADDR"
        private const val LOCAL_PORT = "LOCAL_PORT"
        private const val REMOTE_PORT = "REMOTE_PORT"
        private const val HOST = "HOST"
        private const val EXTRAS = "EXTRAS"

        @JvmStatic
        @Throws(ProtoException::class)
        fun fromJson(jsonStr: String): TunnelRequest {
            val root = try {
                JSONObject(jsonStr)
            } catch (e: Exception) {
                throw ProtoException("解析失败，数据异常", e)
            }
            return TunnelRequest(root)
        }


        fun forTcp(
            localAddr: String,
            localPort: Int,
            remotePort: Int,
            extFunc: (TunnelRequest.() -> Unit)? = null
        ): TunnelRequest {
            return TunnelRequest(
                root = JSONObject()
            ).apply {
                this.protoVersion = Proto.VERSION
                this.tunnelType = TunnelType.TCP
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
                root = JSONObject()
            ).apply {
                this.protoVersion = Proto.VERSION
                this.tunnelType = if (https) TunnelType.HTTPS else TunnelType.HTTP
                this.localAddr = localAddr
                this.localPort = localPort
                this.host = host
                this.extras = JSONObject()
                extFunc?.invoke(this)
            }
        }
    }

    var protoVersion = Proto.VERSION
        private set

    var tunnelType: TunnelType
        get() = TunnelType.findTunnelType(root.getOrDefault(TUNNEL_TYPE, TunnelType.UNKNOWN.value))
        private set(value) = run { root.put(TUNNEL_TYPE, value.value) }

    var localAddr: String
        get() = root.getOrDefault(LOCAL_ADDR, "")
        private set(value) = run { root.put(LOCAL_ADDR, value) }

    var localPort: Int
        get() = root.getOrDefault(LOCAL_PORT, 0)
        private set(value) = run { root.put(LOCAL_PORT, value) }

    var remotePort: Int
        get() = root.getOrDefault(REMOTE_PORT, 0)
        private set(value) = run { root.put(REMOTE_PORT, value) }

    var host: String
        get() = root.getOrDefault(HOST, "")
        set(value) = run { root.put(HOST, value) }

    var extras: JSONObject
        get() = root.getOrDefault(EXTRAS, JSONObject())
        private set(value) = run { root.put(EXTRAS, value) }

    fun copyTcp(
        localAddr: String = this.localAddr,
        localPort: Int = this.localPort,
        remotePort: Int = this.remotePort,
        extFunc: (TunnelRequest.() -> Unit)? = null
    ) = TunnelRequest(this.root).apply {
        this.tunnelType = TunnelType.TCP
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
    ) = TunnelRequest(this.root).apply {
        this.tunnelType = if (https) TunnelType.HTTPS else TunnelType.HTTP
        this.localAddr = localAddr
        this.localPort = localPort
        this.host = host
        extFunc?.invoke(this)
    }

    fun toJsonString(): String = root.toString()

    override fun toString(): String = toString("::")

    fun toString(serverAddr: String): String {
        @Suppress("HttpUrlsUsage")
        return when (tunnelType) {
            TunnelType.TCP -> "tcp://$localAddr:$localPort<-tcp://$serverAddr:$remotePort"
            TunnelType.HTTP -> "http://$localAddr:$localPort<-http://$host"
            TunnelType.HTTPS -> "https://$localAddr:$localPort<-https://$host"
            else -> ""
        }
    }


}
