package cmdclient

import org.apache.log4j.Level
import tpcommon.TPType

data class TPCConfig(
    var basic: Basic = Basic(),
    var log: Log = Log(),
    var ssl: Ssl = Ssl(),
    var tunnels: List<Tunnel> = emptyList()
) {
    data class Basic(
        var worker_threads: Int = -1,
        var auth_token: String? = null,
        var server_addr: String = "127.0.0.1",
        var server_port: Int = 5080
    ) {
        val workerThreads get() = worker_threads
        val authToken get() = auth_token
        val serverAddr get() = server_addr
        val serverPort get() = server_port
    }

    data class Log(
        var level: String = "INFO",
        var file: String = "tpc.log",
        var max_file_size: String = "1MB",
        var max_backup_index: Int = 3
    ) {
        val logLevel: Level
            get() = when (level.trim().toUpperCase()) {
                "ALL" -> Level.ALL
                "TRACE" -> Level.TRACE
                "DEBUG" -> Level.DEBUG
                "INFO" -> Level.INFO
                "WARN" -> Level.WARN
                "ERROR" -> Level.ERROR
                "OFF" -> Level.OFF
                else -> Level.OFF
            }
        val maxFileSize get() = max_file_size
        val maxBackupIndex get() = max_backup_index
    }


    data class Ssl(
        var jks: String? = null,
        var storepass: String? = null,
        var server_port: Int = 5443
    ) {
        val serverPort get() = server_port
    }

    data class Tunnel(
        var type: String = "TCP",
        var enable_ssl: Boolean = false,
        var local_addr: String = "127.0.0.1",
        var local_port: Int = 80,
        var remote_port: Int = 10080,
        var http_host: String = "your.domain.com",
        var http_set_headers: Map<String, String>? = null,
        var http_add_headers: Map<String, String>? = null,
        var http_auth: HttpAuth = HttpAuth()
    ) {
        val tpType: TPType
            get() = when (type.trim().toUpperCase()) {
                "TCP" -> TPType.TCP
                "HTTP" -> TPType.HTTP
                "HTTPS" -> TPType.HTTPS
                else -> TPType.UNKNOWN
            }
        val enableSsl get() = enable_ssl
        val localAddr get() = local_addr
        val localPort get() = local_port
        val remotePort get() = remote_port
        val httpHost get() = http_host
        val httpSetHeaders get() = http_set_headers
        val httpAddHeaders get() = http_add_headers
        val httpAuth get() = http_auth
    }

    data class HttpAuth(
        var enable: Boolean = false,
        var realm: String = ".",
        var username: String = "tp",
        var password: String = "s3cret"
    )
}

