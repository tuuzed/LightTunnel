package lighttunnel.proto

import io.netty.buffer.ByteBufUtil
import io.netty.buffer.Unpooled
import org.json.JSONObject
import java.nio.charset.StandardCharsets

@Suppress("unused")
class TunnelRequest private constructor(
    val type: Type,
    val localAddr: String,
    val localPort: Int,
    private val options: JSONObject
) {

    companion object Factory {
        private val CHARSET = StandardCharsets.UTF_8
        // common
        private const val AUTH_TOKEN = "\$auth_token"
        // tcp
        private const val REMOTE_PORT = "\$remote_port"
        // http & https
        private const val HOST = "\$host"
        private const val ENABLE_BASIC_AUTH = "\$enable_basic_auth"
        private const val BASIC_AUTH_REALM = "\$auth_realm"
        private const val BASIC_AUTH_USERNAME = "\$auth_username"
        private const val BASIC_AUTH_PASSWORD = "\$auth_password"
        private const val PROXY_SET_HEADERS = "\$pxy_set_headers"
        private const val PROXY_ADD_HEADERS = "\$pxy_add_headers"

        @JvmStatic
        @Throws(ProtoException::class)
        fun fromBytes(bytes: ByteArray): TunnelRequest {
            val buffer = Unpooled.wrappedBuffer(bytes)
            try {
                val type = Type.valueOf(buffer.readByte())
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
                buffer.writeByte(type.flag.toInt())
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

        @JvmStatic
        fun forTcp(
            localAddr: String,
            localPort: Int,
            //
            remotePort: Int,
            authToken: String? = null,
            vararg options: Pair<String, String>
        ): TunnelRequest {
            options.forEach { require(!it.first.startsWith("\$")) { "`\$`打头的key为系统保留的key" } }
            val objOptions = JSONObject()
            authToken?.also { objOptions.put(AUTH_TOKEN, it) }
            objOptions.put(REMOTE_PORT, remotePort)
            options.forEach { objOptions.put(it.first, it.second) }
            return TunnelRequest(Type.TCP, localAddr, localPort, objOptions)
        }

        @JvmStatic
        fun forHttp(
            localAddr: String,
            localPort: Int,
            //
            host: String,
            https: Boolean,
            authToken: String?,
            enableBasicAuth: Boolean = false,
            basicAuthRealm: String = ".",
            basicAuthUsername: String = "guest",
            basicAuthPassword: String = "guest",
            proxySetHeaders: Map<String, String> = emptyMap(),
            proxyAddHeaders: Map<String, String> = emptyMap(),
            vararg options: Pair<String, String>
        ): TunnelRequest {
            options.forEach { require(!it.first.startsWith("\$")) { "`\$`打头的key为系统保留的key" } }
            val objOptions = JSONObject()
            authToken?.also { objOptions.put(AUTH_TOKEN, it) }
            objOptions.put(HOST, host)
            if (enableBasicAuth) {
                objOptions.put(ENABLE_BASIC_AUTH, "true")
                objOptions.put(BASIC_AUTH_REALM, basicAuthRealm)
                objOptions.put(BASIC_AUTH_USERNAME, basicAuthUsername)
                objOptions.put(BASIC_AUTH_PASSWORD, basicAuthPassword)
            }
            if (proxySetHeaders.isNotEmpty()) {
                val tmpObj = JSONObject()
                proxySetHeaders.entries.forEach {
                    tmpObj.put(it.key, it.value)
                }
                objOptions.put(PROXY_SET_HEADERS, tmpObj)
            }
            if (proxyAddHeaders.isNotEmpty()) {
                val tmpObj = JSONObject()
                proxyAddHeaders.entries.forEach {
                    tmpObj.put(it.key, it.value)
                }
                objOptions.put(PROXY_ADD_HEADERS, tmpObj)
            }
            options.forEach { objOptions.put(it.first, it.second) }
            return TunnelRequest(if (https) Type.HTTPS else Type.HTTP, localAddr, localPort, objOptions)
        }

    }

    // common
    val authToken = options.getOrDefault<String?>(AUTH_TOKEN, null)
    // tcp
    val remotePort get() = (options.getOrDefault<Int?>(REMOTE_PORT, null) ?: error("remotePort == null"))
    // http & https
    val host get() = options.getOrDefault<String?>(HOST, null) ?: error("host == null")
    val enableBasicAuth = options.getOrDefault(ENABLE_BASIC_AUTH, false)
    val basicAuthRealm = options.getOrDefault(BASIC_AUTH_REALM, ".")
    val basicAuthUsername = options.getOrDefault(BASIC_AUTH_USERNAME, "")
    val basicAuthPassword = options.getOrDefault(BASIC_AUTH_PASSWORD, "")
    val proxySetHeaders by lazy {
        val obj = options.getOrDefault<JSONObject?>(PROXY_SET_HEADERS, null)
        if (obj == null) {
            emptyMap<String, String>()
        } else {
            val map = mutableMapOf<String, String>()
            obj.keys().forEach {
                val value = obj.getOrDefault<String?>(it, null)
                if (value != null) {
                    map[it] = value
                }
            }
            map
        }
    }
    val proxyAddHeaders by lazy {
        val obj = options.getOrDefault<JSONObject?>(PROXY_ADD_HEADERS, null)
        if (obj == null) {
            emptyMap<String, String>()
        } else {
            val map = mutableMapOf<String, String>()
            obj.keys().forEach {
                val value = obj.getOrDefault<String?>(it, null)
                if (value != null) {
                    map[it] = value
                }
            }
            map
        }
    }

    // option
    fun option(key: String): String? = options.getOrDefault<String?>(key, null)

    fun toBytes() = toBytesInternal()

    override fun toString(): String {
        return when (type) {
            Type.TCP -> "$localAddr:$localPort<-tcp://tunnel:$remotePort #options:$options"
            Type.HTTP -> "$localAddr:$localPort<-http://$host #options:$options"
            Type.HTTPS -> "$localAddr:$localPort<-https://$host #options:$options"
            else -> ""
        }
    }

    enum class Type(val flag: Byte) {
        UNKNOWN(0x00.toByte()),
        TCP(0x10.toByte()),
        HTTP(0x30.toByte()),
        HTTPS(0x31.toByte());

        companion object {
            @JvmStatic
            fun valueOf(flag: Byte) = values().firstOrNull { it.flag == flag } ?: UNKNOWN
        }

    }

    private inline fun <reified T> JSONObject.getOrDefault(key: String, def: T): T {
        if (this.has(key)) {
            val value = this.get(key)
            if (value is T) {
                return value
            }
            return def
        } else {
            return def
        }
    }

}
