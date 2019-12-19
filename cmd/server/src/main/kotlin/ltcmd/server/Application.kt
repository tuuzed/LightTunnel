package ltcmd.server

import io.netty.handler.ssl.SslContext
import lighttunnel.cmd.CmdLineParser
import lighttunnel.logging.LoggerFactory
import lighttunnel.proto.LTManifest
import lighttunnel.server.LTServer
import lighttunnel.server.LTSimpleRequestInterceptor
import lighttunnel.util.SslContextUtil
import org.apache.log4j.Level
import org.apache.log4j.helpers.OptionConverter
import org.ini4j.Ini
import org.ini4j.Profile
import java.io.File

class Application {

    fun doMain(args: Array<String>) {
        val cmdLine = CmdLine()
        CmdLineParser.parse(cmdLine, args)
        if (cmdLine.help) {
            System.err.printf("%n%nUsage: %n")
            CmdLineParser.printHelp(cmdLine, System.err, "    ")
            return
        }
        val ini = Ini()
        ini.load(File(cmdLine.iniFile))
        setupLog(ini["log"])
        val basic = ini["basic"] ?: return
        val tcp = ini["tcp"]
        val ssl = ini["ssl"]
        val http = ini["http"]
        val https = ini["https"]
        val authToken = basic["auth_token"] ?: ""
        val tcpAllowPorts = tcp?.get("allow_ports") ?: ""
        val interceptor = LTSimpleRequestInterceptor(authToken, tcpAllowPorts)
        val sslEnable = ssl?.get("enable")?.toUpperCase() == "TRUE"
        val sslContext: SslContext? = if (sslEnable) {
            val jks = ssl?.get("jks")
            val storePassword = ssl?.get("store_password")
            val keyPassword = ssl?.get("key_password")
            requireNotNull(jks)
            requireNotNull(storePassword)
            requireNotNull(keyPassword)
            SslContextUtil.forServer(jks, storePassword, keyPassword)
        } else {
            null
        }
        val httpsEnable = https?.get("enable")?.toUpperCase() == "TRUE"
        val httpsContext: SslContext? = if (httpsEnable) {
            val jks = https?.get("jks")
            val storePassword = https?.get("store_password")
            val keyPassword = https?.get("key_password")
            requireNotNull(jks)
            requireNotNull(storePassword)
            requireNotNull(keyPassword)
            SslContextUtil.forServer(jks, storePassword, keyPassword)
        } else {
            null
        }
        val options = LTServer.Options(
            bossThreads = basic["boss_threads"].int() ?: -1,
            workerThreads = basic["worker_threads"].int() ?: -1,
            // tunnel
            tpRequestInterceptor = interceptor,
            bindAddr = basic["bind_addr"] ?: "0.0.0.0",
            bindPort = basic["bind_port"].int() ?: 5080,
            sslEnable = sslEnable,
            sslContext = sslContext,
            sslBindAddr = ssl?.get("bind_addr") ?: "0.0.0.0",
            sslBindPort = ssl?.get("bind_port").int() ?: 5080,
            // tcp
            tcpEnable = tcp?.get("enable")?.toUpperCase() == "TRUE",
            // http
            httpEnable = http?.get("enable")?.toUpperCase() == "TRUE",
            httpBindAddr = http?.get("bind_addr") ?: "0.0.0.0",
            httpBindPort = http?.get("bind_port").int() ?: 80,
            tpHttpRequestInterceptor = interceptor,
            // https
            httpsEnable = https?.get("enable")?.toUpperCase() == "TRUE",
            httpsContext = httpsContext,
            httpsBindAddr = https?.get("bind_addr") ?: "0.0.0.0",
            httpsBindPort = https?.get("bind_port").int() ?: 80,
            tpHttpsRequestInterceptor = interceptor
        )
        LTServer(options).start()
    }

    private fun setupLog(log: Profile.Section?) {
        var level = Level.OFF
        var file: String? = null
        var maxBackupIndex: Int? = null
        var maxFileSize: String? = null
        if (log != null) {
            level = Level.toLevel(log["level"])
            file = log["file"] ?: ""
            maxBackupIndex = log["max_backup_index"].int()
            maxFileSize = log["max_file_size"]
        }
        LoggerFactory.configConsole(Level.OFF, names = *LTManifest.thirdLibs)
        LoggerFactory.configConsole(level = level)
        LoggerFactory.configFile(
            level = level,
            file = file ?: "./logs/lts.log",
            maxBackupIndex = maxBackupIndex ?: 0,
            maxFileSize = OptionConverter.toFileSize(maxFileSize, 1)
        )
    }

    private fun String?.int(): Int? {
        return try {
            this?.toInt()
        } catch (e: NumberFormatException) {
            return null
        }
    }

}