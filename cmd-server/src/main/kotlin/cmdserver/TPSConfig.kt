package cmdserver

import org.apache.log4j.Level

data class TPSConfig(
    var basic: Basic = Basic(),
    var log: Log = Log(),
    var ssl: Ssl = Ssl(),
    var tcp: Tcp = Tcp(),
    var http: Http = Http(),
    var https: Https = Https()
) {

    data class Basic(
        var boss_threads: Int = -1,
        var worker_threads: Int = -1,
        var auth_token: String? = null,
        var bind_addr: String? = null,
        var bind_port: Int = 5080
    ) {
        val bossThreads get() = boss_threads
        val workerThreads get() = worker_threads
        val authToken get() = auth_token
        val bindAddr get() = bind_addr
        val bindPort get() = bind_port
    }

    data class Log(
        var level: String = "INFO",
        var file: String = "tps.log",
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
        var enable: Boolean = false,
        var jks: String? = null,
        var storepass: String? = null,
        var keypass: String? = null,
        var bind_addr: String? = null,
        var bind_port: Int = 5080
    ) {
        val bindAddr get() = bind_addr
        val bindPort get() = bind_port
    }

    data class Tcp(
        var enable: Boolean = true,
        var allow_ports: String? = null
    ) {
        val allowPorts get() = allow_ports
    }

    data class Http(
        var enable: Boolean = false,
        var bind_addr: String? = null,
        var bind_port: Int = 80
    ) {
        val bindAddr get() = bind_addr
        val bindPort get() = bind_port
    }

    data class Https(
        var enable: Boolean = false,
        var jks: String? = null,
        var storepass: String? = null,
        var keypass: String? = null,
        var bind_addr: String? = null,
        var bind_port: Int = 443
    ) {
        val bindAddr get() = bind_addr
        val bindPort get() = bind_port
    }
}




