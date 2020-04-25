@file:Suppress("DuplicatedCode")

package lighttunnel.proto

import io.netty.buffer.ByteBufUtil
import io.netty.buffer.Unpooled
import lighttunnel.util.json.getOrDefault
import lighttunnel.util.json.toStringMap
import org.json.JSONObject
import java.nio.charset.StandardCharsets

@Suppress("unused")
data class TunnelRequest internal constructor(
    val type: Type,
    val localAddr: String,
    val localPort: Int,
    private val options: JSONObject
) {

    companion object Factory {
        private val CHARSET = StandardCharsets.UTF_8

        // common
        private const val NAME = "\$name"
        private const val AUTH_TOKEN = "\$auth_token"

        // tcp
        private const val REMOTE_PORT = "\$remote_port"

        // http & https
        private const val HOST = "\$host"
        private const val ENABLE_BASIC_AUTH = "\$enable_basic_auth"
        private const val BASIC_AUTH_REALM = "\$auth_realm"
        private const val BASIC_AUTH_USERNAME = "\$auth_username"
        private const val BASIC_AUTH_PASSWORD = "\$auth_password"
        private const val PXY_SET_HEADERS = "\$pxy_set_headers"
        private const val PXY_ADD_HEADERS = "\$pxy_add_headers"

        @Throws(ProtoException::class)
        fun fromBytes(bytes: ByteArray): TunnelRequest {
            val buffer = Unpooled.wrappedBuffer(bytes)
            try {
                val type = Type.ofCode(buffer.readByte())
                val localPort = buffer.readInt()
                val localAddrBytes = ByteArray(buffer.readInt())
                buffer.readBytes(localAddrBytes)
                val localAddr = String(localAddrBytes, CHARSET)
                val optionsBytes = ByteArray(buffer.readInt())
                buffer.readBytes(optionsBytes)
                val options = JSONObject(String(optionsBytes, CHARSET))
                return TunnelRequest(type, localAddr, localPort, options)
            } catch (e: Exception) {
                throw ProtoException("解析失败，数据异常", e)
            } finally {
                buffer.release()
            }
        }

        private fun TunnelRequest.toBytesInternal(): ByteArray {
            val buffer = Unpooled.buffer()
            try {
                buffer.writeByte(type.code.toInt())
                buffer.writeInt(localPort)
                localAddr.toByteArray(CHARSET).also {
                    buffer.writeInt(it.size)
                    buffer.writeBytes(it)
                }
                options.toString().toByteArray(CHARSET).also {
                    buffer.writeInt(it.size)
                    buffer.writeBytes(it)
                }
                return ByteBufUtil.getBytes(buffer)
            } finally {
                buffer.release()
            }
        }

        fun forTcp(
            localAddr: String,
            localPort: Int,
            remotePort: Int,
            name: String? = null,
            authToken: String? = null,
            vararg options: Pair<String, String>
        ): TunnelRequest {
            options.forEach { require(!it.first.startsWith("\$")) { "`\$`打头的key为系统保留的key" } }
            val objOptions = JSONObject()
            name?.also { objOptions.put(NAME, it) }
            authToken?.also { objOptions.put(AUTH_TOKEN, it) }
            objOptions.put(REMOTE_PORT, remotePort)
            options.forEach { objOptions.put(it.first, it.second) }
            return TunnelRequest(type = Type.TCP, localAddr = localAddr, localPort = localPort, options = objOptions)
        }

        fun TunnelRequest.copyTcp(
            localAddr: String = this.localAddr,
            localPort: Int = this.localPort,
            remotePort: Int = this.remotePort,
            name: String? = this.name,
            authToken: String? = this.authToken,
            vararg options: Pair<String, String> = this.options().map { it.key to it.value }.toTypedArray()
        ) = forTcp(
            localAddr = localAddr,
            localPort = localPort,
            remotePort = remotePort,
            name = name,
            authToken = authToken,
            options = *options
        )

        fun forHttp(
            localAddr: String,
            localPort: Int,
            host: String,
            https: Boolean,
            name: String? = null,
            authToken: String? = null,
            enableBasicAuth: Boolean = false,
            basicAuthRealm: String = ".",
            basicAuthUsername: String = "guest",
            basicAuthPassword: String = "guest",
            pxySetHeaders: Map<String, String> = emptyMap(),
            pxyAddHeaders: Map<String, String> = emptyMap(),
            vararg options: Pair<String, String>
        ): TunnelRequest {
            options.forEach { require(!it.first.startsWith("\$")) { "`\$`打头的key为系统保留的key" } }
            val objOptions = JSONObject()
            name?.also { objOptions.put(NAME, it) }
            authToken?.also { objOptions.put(AUTH_TOKEN, it) }
            objOptions.put(HOST, host)
            if (enableBasicAuth) {
                objOptions.put(ENABLE_BASIC_AUTH, "true")
                objOptions.put(BASIC_AUTH_REALM, basicAuthRealm)
                objOptions.put(BASIC_AUTH_USERNAME, basicAuthUsername)
                objOptions.put(BASIC_AUTH_PASSWORD, basicAuthPassword)
            }
            if (pxySetHeaders.isNotEmpty()) {
                val tmpObj = JSONObject()
                pxySetHeaders.entries.forEach {
                    tmpObj.put(it.key, it.value)
                }
                objOptions.put(PXY_SET_HEADERS, tmpObj)
            }
            if (pxyAddHeaders.isNotEmpty()) {
                val tmpObj = JSONObject()
                pxyAddHeaders.entries.forEach {
                    tmpObj.put(it.key, it.value)
                }
                objOptions.put(PXY_ADD_HEADERS, tmpObj)
            }
            options.forEach { objOptions.put(it.first, it.second) }
            return TunnelRequest(
                type = if (https) Type.HTTPS else Type.HTTP,
                localAddr = localAddr,
                localPort = localPort,
                options = objOptions
            )
        }

        fun TunnelRequest.copyHttp(
            localAddr: String = this.localAddr,
            localPort: Int = this.localPort,
            host: String = this.host,
            https: Boolean = this.type == Type.HTTPS,
            name: String? = this.name,
            authToken: String? = this.authToken,
            enableBasicAuth: Boolean = this.enableBasicAuth,
            basicAuthRealm: String = this.basicAuthRealm,
            basicAuthUsername: String = this.basicAuthUsername,
            basicAuthPassword: String = this.basicAuthPassword,
            pxySetHeaders: Map<String, String> = this.pxySetHeaders,
            pxyAddHeaders: Map<String, String> = this.pxyAddHeaders,
            vararg options: Pair<String, String> = this.options().map { it.key to it.value }.toTypedArray()
        ) = forHttp(
            localAddr = localAddr,
            localPort = localPort,
            host = host,
            https = https,
            name = name,
            authToken = authToken,
            enableBasicAuth = enableBasicAuth,
            basicAuthRealm = basicAuthRealm,
            basicAuthUsername = basicAuthUsername,
            basicAuthPassword = basicAuthPassword,
            pxyAddHeaders = pxySetHeaders,
            pxySetHeaders = pxyAddHeaders,
            options = *options
        )

    }

    // common
    val name get() = options.getOrDefault<String?>(NAME, null)
    val authToken get() = options.getOrDefault<String?>(AUTH_TOKEN, null)

    // tcp
    val remotePort get() = (options.getOrDefault<Int?>(REMOTE_PORT, null) ?: error("remotePort == null"))

    // http & https
    val host get() = options.getOrDefault<String?>(HOST, null) ?: error("host == null")
    val enableBasicAuth get() = options.getOrDefault(ENABLE_BASIC_AUTH, false)
    val basicAuthRealm get() = options.getOrDefault(BASIC_AUTH_REALM, ".")
    val basicAuthUsername get() = options.getOrDefault(BASIC_AUTH_USERNAME, "")
    val basicAuthPassword get() = options.getOrDefault(BASIC_AUTH_PASSWORD, "")
    val pxySetHeaders by lazy { options.getOrDefault<JSONObject?>(PXY_SET_HEADERS, null).toStringMap() }
    val pxyAddHeaders by lazy { options.getOrDefault<JSONObject?>(PXY_ADD_HEADERS, null).toStringMap() }

    // option
    fun option(key: String): String? = options.getOrDefault<String?>(key, null)

    private fun options() = options.toStringMap().filterNot { it.key.startsWith("\$") }

    fun toBytes() = toBytesInternal()

    override fun toString(): String {
        return toString("tunnel")
    }

    val optionsString get() = options.toString()

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
            fun ofCode(code: Byte) = values().firstOrNull { it.code == code } ?: UNKNOWN
        }

    }

}
