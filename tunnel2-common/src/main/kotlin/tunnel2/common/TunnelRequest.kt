package tunnel2.common

import io.netty.buffer.Unpooled
import java.nio.charset.StandardCharsets
import java.util.*

class TunnelRequest private constructor(
    val type: TunnelType,
    val localAddr: String,
    val localPort: Int,
    private val options: Map<String, String>
) {

    // common
    val authToken: String? get() = options[AUTH_TOKEN]
    // tcp
    val remotePort: Int get() = (options[REMOTE_PORT] ?: error("remotePort == null")).toInt()
    // http & https
    val vhost: String get() = options[VHOST] ?: error("vhost == null")
    val enableBasicAuth: Boolean = options[ENABLE_BASIC_AUTH] == "1"
    val basicAuthRealm: String = options[BASIC_AUTH_REALM] ?: "."
    val basicAuthUsername: String = options[BASIC_AUTH_USERNAME] ?: ""
    val basicAuthPassword: String = options[BASIC_AUTH_PASSWORD] ?: ""
    val proxySetHeaders: Map<String, String> = parseHeadersOption(options[PROXY_SET_HEADERS])
    val proxyAddHeaders: Map<String, String> = parseHeadersOption(options[PROXY_ADD_HEADERS])

    // added Option
    fun getAddedOption(key: String): String? = options[key]

    override fun toString(): String {
        return when (type) {
            TunnelType.TCP -> "[ $localAddr:$localPort<-tcp://{server}:$remotePort { ${optionsMapToLine(options)} } ]"
            TunnelType.UDP -> "[ $localAddr:$localPort<-udp://{server}:$remotePort { ${optionsMapToLine(options)} } ]"
            TunnelType.HTTP -> String.format("[ $localAddr:$localPort<-http://$vhost { ${optionsMapToLine(options)} } ]", localAddr, localPort, vhost, optionsMapToLine(options))
            TunnelType.HTTPS -> String.format("[ $localAddr:$localPort<-https://$vhost { ${optionsMapToLine(options)} } ]", localAddr, localPort, vhost, optionsMapToLine(options))
            else -> ""
        }
    }

    fun toBytes(): ByteArray {
        val buffer = Unpooled.buffer()
        buffer.writeByte(type.value.toInt())
        buffer.writeInt(localPort)

        val loadAddrBytes = localAddr.toByteArray(StandardCharsets.UTF_8)
        buffer.writeInt(loadAddrBytes.size)
        buffer.writeBytes(loadAddrBytes)

        val optionsBytes = optionsMapToLine(options).toByteArray(StandardCharsets.UTF_8)
        buffer.writeInt(optionsBytes.size)
        buffer.writeBytes(optionsBytes)

        val bytes = ByteArray(buffer.readableBytes())
        buffer.readBytes(bytes)

        buffer.release()
        return bytes
    }

    companion object {
        // common
        private const val AUTH_TOKEN = "_AT_"
        // tcp
        private const val REMOTE_PORT = "_RP_"
        // http & https
        private const val VHOST = "_VH_"
        private const val ENABLE_BASIC_AUTH = "_AU_"
        private const val BASIC_AUTH_REALM = "_AR_"
        private const val BASIC_AUTH_USERNAME = "_AS_"
        private const val BASIC_AUTH_PASSWORD = "_AP_"
        private const val PROXY_SET_HEADERS = "_SH_"
        private const val PROXY_ADD_HEADERS = "_AH_"

        @JvmStatic
        @Throws(TunnelException::class)
        fun fromBytes(bytes: ByteArray): TunnelRequest {
            val buffer = Unpooled.wrappedBuffer(bytes)
            try {
                val proto = TunnelType.ofValue(buffer.readByte())
                val localPort = buffer.readInt()

                val loadAddrBytes = ByteArray(buffer.readInt())
                buffer.readBytes(loadAddrBytes)
                val loadAddr = String(loadAddrBytes, StandardCharsets.UTF_8)

                val optionsBytes = ByteArray(buffer.readInt())
                buffer.readBytes(optionsBytes)
                val options = optionsLineToMap(String(optionsBytes, StandardCharsets.UTF_8))

                return TunnelRequest(proto, loadAddr, localPort, options)
            } catch (e: Exception) {
                throw TunnelException("解析失败，数据异常", e)
            } finally {
                buffer.release()
            }
        }

        @JvmStatic
        fun ofTcp(
            localAddr: String,
            localPort: Int,
            remotePort: Int,
            authToken: String? = null,
            vararg addedOptions: Pair<String, String>
        ): TunnelRequest {
            checkAddedOptions(*addedOptions)
            val options = mutableMapOf(
                Pair(REMOTE_PORT, remotePort.toString()),
                *addedOptions
            )
            authToken?.also { options[AUTH_TOKEN] = authToken }
            return TunnelRequest(TunnelType.TCP, localAddr, localPort, options)
        }

        @JvmStatic
        fun ofUdp(
            localAddr: String,
            localPort: Int,
            remotePort: Int,
            authToken: String? = null,
            vararg addedOptions: Pair<String, String>
        ): TunnelRequest {
            checkAddedOptions(*addedOptions)
            val options = mutableMapOf(
                Pair(REMOTE_PORT, remotePort.toString()),
                *addedOptions
            )
            authToken?.also { options[AUTH_TOKEN] = authToken }
            return TunnelRequest(TunnelType.UDP, localAddr, localPort, options)
        }


        @JvmStatic
        fun ofHttp(
            localAddr: String,
            localPort: Int,
            vhost: String,
            enableBasicAuth: Boolean = false,
            basicAuthRealm: String = ".",
            basicAuthUsername: String = "",
            basicAuthPassword: String = "",
            proxySetHeaders: Map<String, String> = emptyMap(),
            proxyAddHeaders: Map<String, String> = emptyMap(),
            authToken: String? = null,
            vararg addedOptions: Pair<String, String>
        ): TunnelRequest {
            checkAddedOptions(*addedOptions)
            val options = mutableMapOf(
                Pair(VHOST, vhost),
                Pair(ENABLE_BASIC_AUTH, if (enableBasicAuth) "1" else "0"),
                *addedOptions
            )
            if (enableBasicAuth) {
                options[BASIC_AUTH_REALM] = basicAuthRealm
                options[BASIC_AUTH_USERNAME] = basicAuthUsername
                options[BASIC_AUTH_PASSWORD] = basicAuthPassword
            }
            setHeadersOption(PROXY_SET_HEADERS, proxySetHeaders, options)
            setHeadersOption(PROXY_ADD_HEADERS, proxyAddHeaders, options)
            authToken?.also { options[AUTH_TOKEN] = authToken }
            return TunnelRequest(TunnelType.HTTP, localAddr, localPort, options)
        }

        @JvmStatic
        fun ofHttps(
            localAddr: String,
            localPort: Int,
            vhost: String,
            enableBasicAuth: Boolean = false,
            basicAuthRealm: String = ".",
            basicAuthUsername: String = "",
            basicAuthPassword: String = "",
            proxySetHeaders: Map<String, String> = emptyMap(),
            proxyAddHeaders: Map<String, String> = emptyMap(),
            authToken: String? = null,
            vararg addedOptions: Pair<String, String>
        ): TunnelRequest {
            checkAddedOptions(*addedOptions)
            val options = mutableMapOf(
                Pair(VHOST, vhost),
                Pair(ENABLE_BASIC_AUTH, if (enableBasicAuth) "1" else "0"),
                *addedOptions
            )
            if (enableBasicAuth) {
                options[BASIC_AUTH_REALM] = basicAuthRealm
                options[BASIC_AUTH_USERNAME] = basicAuthUsername
                options[BASIC_AUTH_PASSWORD] = basicAuthPassword
            }
            setHeadersOption(PROXY_SET_HEADERS, proxySetHeaders, options)
            setHeadersOption(PROXY_ADD_HEADERS, proxyAddHeaders, options)
            authToken?.also { options[AUTH_TOKEN] = authToken }
            return TunnelRequest(TunnelType.HTTPS, localAddr, localPort, options)
        }

        private fun checkAddedOptions(vararg addedOptions: Pair<String, String>) {
            addedOptions.forEach {
                require(!(it.first.startsWith("_") && it.first.endsWith("_"))) { "`_`打头`_`结尾的key为系统保留的key" }
            }
        }

        private fun setHeadersOption(option: String, headers: Map<String, String>, options: MutableMap<String, String>) {
            if (headers.isEmpty()) {
                return
            }
            options[option] = headers.entries.joinToString(separator = ";") { "${it.key}:${it.value}" }
        }

        private fun parseHeadersOption(headers: String?): Map<String, String> {
            headers ?: return emptyMap()
            val map = LinkedHashMap<String, String>()
            for (it in headers.split(";".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()) {
                val line = it.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                if (line.size == 2) {
                    map[line[0]] = line[1]
                }
            }
            return map
        }

        private fun optionsLineToMap(originalLine: String?): Map<String, String> {
            originalLine ?: return emptyMap()
            val kvLines = originalLine.split("&".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            val map = LinkedHashMap<String, String>(kvLines.size)
            for (it in kvLines) {
                val kvLine = it.split("=".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                if (kvLine.size == 1) {
                    map[kvLine[0]] = ""
                } else if (kvLine.size == 2) {
                    map[kvLine[0]] = kvLine[1]
                }
            }
            return map
        }

        private fun optionsMapToLine(originalMap: Map<String, String>?): String {
            originalMap ?: return ""
            return originalMap.entries.joinToString(separator = "&") { "${it.key}=${it.value}" }
        }

    }

}
