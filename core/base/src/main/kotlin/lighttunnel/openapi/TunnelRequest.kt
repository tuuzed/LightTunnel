@file:Suppress("unused", "DuplicatedCode")

package lighttunnel.openapi

import io.netty.buffer.ByteBufUtil
import io.netty.buffer.Unpooled
import lighttunnel.base.util.getOrDefault
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

        private val CHARSET = Charsets.UTF_8

        // common
        private const val NAME = "\$name"
        private const val AUTH_TOKEN = "\$auth_token"
        private const val VERSION = "\$version"

        // tcp
        private const val REMOTE_PORT = "\$remote_port"

        // http & https
        private const val HOST = "\$host"
        private const val ENABLE_BASIC_AUTH = "\$enable_basic_auth"
        private const val BASIC_AUTH_REALM = "\$basic_auth_realm"
        private const val BASIC_AUTH_USERNAME = "\$basic_auth_username"
        private const val BASIC_AUTH_PASSWORD = "\$basic_auth_password"
        private const val PXY_SET_HEADERS = "\$pxy_set_headers"
        private const val PXY_ADD_HEADERS = "\$pxy_add_headers"

        @Throws(ProtoException::class)
        fun fromBytes(bytes: ByteArray): TunnelRequest {
            val buffer = Unpooled.wrappedBuffer(bytes)
            try {
                val type = Type.codeOf(buffer.readByte())
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

        fun forTcp(
            localAddr: String,
            localPort: Int,
            remotePort: Int,
            name: String? = null,
            authToken: String? = null,
            vararg extras: Pair<String, String>
        ): TunnelRequest {
            extras.forEach { require(!it.first.startsWith("\$")) { "`\$`打头的key为系统保留的key" } }
            val finalExtras = JSONObject()
            finalExtras.put(REMOTE_PORT, remotePort)
            name?.also { finalExtras.put(NAME, it) }
            authToken?.also { finalExtras.put(AUTH_TOKEN, it) }
            extras.forEach { finalExtras.put(it.first, it.second) }
            finalExtras.put(VERSION, BuildConfig.VERSION)
            return TunnelRequest(type = Type.TCP, localAddr = localAddr, localPort = localPort, extras = finalExtras)
        }

        fun forHttp(
            https: Boolean = false,
            localAddr: String,
            localPort: Int,
            host: String,
            name: String? = null,
            authToken: String? = null,
            enableBasicAuth: Boolean = false,
            basicAuthRealm: String = ".",
            basicAuthUsername: String = "guest",
            basicAuthPassword: String = "guest",
            pxySetHeaders: Map<String, String> = emptyMap(),
            pxyAddHeaders: Map<String, String> = emptyMap(),
            vararg extras: Pair<String, String>
        ): TunnelRequest {
            extras.forEach { require(!it.first.startsWith("\$")) { "`\$`打头的key为系统保留的key" } }
            val finalExtras = JSONObject()
            finalExtras.put(HOST, host)
            name?.also { finalExtras.put(NAME, it) }
            authToken?.also { finalExtras.put(AUTH_TOKEN, it) }
            if (enableBasicAuth) {
                finalExtras.put(ENABLE_BASIC_AUTH, "true")
                finalExtras.put(BASIC_AUTH_REALM, basicAuthRealm)
                finalExtras.put(BASIC_AUTH_USERNAME, basicAuthUsername)
                finalExtras.put(BASIC_AUTH_PASSWORD, basicAuthPassword)
            }
            if (pxySetHeaders.isNotEmpty()) {
                val tmpObj = JSONObject()
                pxySetHeaders.entries.forEach { tmpObj.put(it.key, it.value) }
                finalExtras.put(PXY_SET_HEADERS, tmpObj)
            }
            if (pxyAddHeaders.isNotEmpty()) {
                val tmpObj = JSONObject()
                pxyAddHeaders.entries.forEach { tmpObj.put(it.key, it.value) }
                finalExtras.put(PXY_ADD_HEADERS, tmpObj)
            }
            extras.forEach { finalExtras.put(it.first, it.second) }
            finalExtras.put(VERSION, BuildConfig.VERSION)
            val type = if (https) Type.HTTPS else Type.HTTP
            return TunnelRequest(type = type, localAddr = localAddr, localPort = localPort, extras = finalExtras)
        }
    }

    // common
    val name get() = extras.getOrDefault<String?>(NAME, null)
    val authToken get() = extras.getOrDefault<String?>(AUTH_TOKEN, null)
    val version get() = extras.getOrDefault<String?>(VERSION, null)

    // tcp
    val remotePort get() = (extras.getOrDefault<Int?>(REMOTE_PORT, null) ?: error("remotePort == null"))

    // http & https
    val host get() = extras.getOrDefault<String?>(HOST, null) ?: error("host == null")
    val enableBasicAuth get() = extras.getOrDefault(ENABLE_BASIC_AUTH, "false").toUpperCase() == "TRUE"
    val basicAuthRealm get() = extras.getOrDefault(BASIC_AUTH_REALM, ".")
    val basicAuthUsername get() = extras.getOrDefault(BASIC_AUTH_USERNAME, "")
    val basicAuthPassword get() = extras.getOrDefault(BASIC_AUTH_PASSWORD, "")
    val pxySetHeaders by lazy { extras.getOrDefault<JSONObject?>(PXY_SET_HEADERS, null).toStringMap() }
    val pxyAddHeaders by lazy { extras.getOrDefault<JSONObject?>(PXY_ADD_HEADERS, null).toStringMap() }

    // extra
    fun getExtra(key: String) = extras.getOrDefault<String?>(key, null)

    fun toBytes() = toBytesInternal()

    fun copyTcp(
        localAddr: String = this.localAddr,
        localPort: Int = this.localPort,
        remotePort: Int = this.remotePort,
        name: String? = this.name,
        authToken: String? = this.authToken,
        vararg extras: Pair<String, String> = this.extras().map { it.key to it.value }.toTypedArray()
    ) = forTcp(
        localAddr = localAddr,
        localPort = localPort,
        remotePort = remotePort,
        name = name,
        authToken = authToken,
        extras = *extras
    )

    fun copyHttp(
        https: Boolean = this.type == Type.HTTPS,
        localAddr: String = this.localAddr,
        localPort: Int = this.localPort,
        host: String = this.host,
        name: String? = this.name,
        authToken: String? = this.authToken,
        enableBasicAuth: Boolean = this.enableBasicAuth,
        basicAuthRealm: String = this.basicAuthRealm,
        basicAuthUsername: String = this.basicAuthUsername,
        basicAuthPassword: String = this.basicAuthPassword,
        pxySetHeaders: Map<String, String> = this.pxySetHeaders,
        pxyAddHeaders: Map<String, String> = this.pxyAddHeaders,
        vararg extras: Pair<String, String> = this.extras().map { it.key to it.value }.toTypedArray()
    ) = forHttp(
        https = https,
        localAddr = localAddr,
        localPort = localPort,
        host = host,
        name = name,
        authToken = authToken,
        enableBasicAuth = enableBasicAuth,
        basicAuthRealm = basicAuthRealm,
        basicAuthUsername = basicAuthUsername,
        basicAuthPassword = basicAuthPassword,
        pxyAddHeaders = pxySetHeaders,
        pxySetHeaders = pxyAddHeaders,
        extras = *extras
    )

    override fun toString(): String {
        return toString("::")
    }

    val optionsString get() = extras.toString()

    fun toString(serverAddr: String): String {
        return when (type) {
            Type.TCP -> "tcp://$localAddr:$localPort<-tcp://$serverAddr:$remotePort"
            Type.HTTP -> "http://$localAddr:$localPort<-http://$host"
            Type.HTTPS -> "https://$localAddr:$localPort<-https://$host"
            else -> ""
        }
    }

    private fun toBytesInternal(): ByteArray {
        val buffer = Unpooled.buffer()
        try {
            buffer.writeByte(type.code.toInt())
            buffer.writeInt(localPort)
            localAddr.toByteArray(CHARSET).also {
                buffer.writeInt(it.size)
                buffer.writeBytes(it)
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

    private fun extras() = extras.toStringMap().filterNot { it.key.startsWith("\$") }

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
