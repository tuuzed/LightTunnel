@file:Suppress("unused")

package lighttunnel.common.entity

import lighttunnel.common.exception.LightTunnelException
import lighttunnel.common.utils.getOrDefault
import org.json.JSONObject
import java.io.Serializable

class TunnelRequest private constructor(
    private val root: JSONObject
) : Serializable {

    companion object {
        private const val serialVersionUID = 1L
        private const val TUNNEL_TYPE = "TUNNEL_TYPE"
        private const val LOCAL_IP = "LOCAL_IP"
        private const val LOCAL_PORT = "LOCAL_PORT"
        private const val REMOTE_PORT = "REMOTE_PORT"
        private const val VHOST = "VHOST"
        private const val EXTRAS = "EXTRAS"


        @Throws(LightTunnelException::class)
        fun internalFromJson(jsonStr: String): TunnelRequest {
            val root = try {
                JSONObject(jsonStr)
            } catch (e: Exception) {
                throw LightTunnelException("解析失败，数据异常", e)
            }
            return TunnelRequest(root)
        }

        fun forTcp(
            localIp: String,
            localPort: Int,
            remotePort: Int,
            extFunc: (TunnelRequest.() -> Unit)? = null
        ): TunnelRequest {
            return TunnelRequest(
                root = JSONObject()
            ).apply {
                this.tunnelType = TunnelType.TCP
                this.localIp = localIp
                this.localPort = localPort
                this.remotePort = remotePort
                this.extras = JSONObject()
                extFunc?.invoke(this)
            }
        }

        fun forHttp(
            https: Boolean = false,
            localIp: String,
            localPort: Int,
            vhost: String,
            extFunc: (TunnelRequest.() -> Unit)? = null
        ): TunnelRequest {
            return TunnelRequest(
                root = JSONObject()
            ).apply {
                this.tunnelType = if (https) TunnelType.HTTPS else TunnelType.HTTP
                this.localIp = localIp
                this.localPort = localPort
                this.vhost = vhost
                this.extras = JSONObject()
                extFunc?.invoke(this)
            }
        }
    }

    var tunnelType: TunnelType
        get() = TunnelType.findTunnelType(root.getOrDefault(TUNNEL_TYPE, TunnelType.UNKNOWN.value))
        private set(value) = run { root.put(TUNNEL_TYPE, value.value) }

    var localIp: String
        get() = root.getOrDefault(LOCAL_IP, "")
        private set(value) = run { root.put(LOCAL_IP, value) }

    var localPort: Int
        get() = root.getOrDefault(LOCAL_PORT, 0)
        private set(value) = run { root.put(LOCAL_PORT, value) }

    var remotePort: Int
        get() = root.getOrDefault(REMOTE_PORT, 0)
        private set(value) = run { root.put(REMOTE_PORT, value) }

    var vhost: String
        get() = root.getOrDefault(VHOST, "")
        set(value) = run { root.put(VHOST, value) }

    var extras: JSONObject
        get() = root.getOrDefault(EXTRAS, JSONObject())
        private set(value) = run { root.put(EXTRAS, value) }

    fun copyTcp(
        localIp: String = this.localIp,
        localPort: Int = this.localPort,
        remotePort: Int = this.remotePort,
        extFunc: (TunnelRequest.() -> Unit)? = null
    ) = TunnelRequest(this.root).apply {
        this.tunnelType = TunnelType.TCP
        this.localIp = localIp
        this.localPort = localPort
        this.remotePort = remotePort
        extFunc?.invoke(this)
    }

    fun copyHttp(
        https: Boolean = this.tunnelType == TunnelType.HTTPS,
        localIp: String = this.localIp,
        localPort: Int = this.localPort,
        vhost: String = this.vhost,
        extFunc: (TunnelRequest.() -> Unit)? = null
    ) = TunnelRequest(this.root).apply {
        this.tunnelType = if (https) TunnelType.HTTPS else TunnelType.HTTP
        this.localIp = localIp
        this.localPort = localPort
        this.vhost = vhost
        extFunc?.invoke(this)
    }

    fun asJsonString(): String = root.toString()

    override fun toString(): String = toString("::")

    fun toString(serverIp: String): String {
        @Suppress("HttpUrlsUsage")
        return when (tunnelType) {
            TunnelType.TCP -> "tcp://$localIp:$localPort<-tcp://$serverIp:$remotePort"
            TunnelType.HTTP -> "http://$localIp:$localPort<-http://$vhost"
            TunnelType.HTTPS -> "https://$localIp:$localPort<-https://$vhost"
            else -> ""
        }
    }


}
