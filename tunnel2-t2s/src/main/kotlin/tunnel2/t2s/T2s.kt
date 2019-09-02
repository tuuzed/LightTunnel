package tunnel2.t2s

import io.netty.handler.ssl.SslContext
import org.apache.log4j.Level
import org.apache.log4j.helpers.OptionConverter
import org.yaml.snakeyaml.Yaml
import tunnel2.common.logging.LoggerFactory
import tunnel2.common.ssl.SslContexts
import tunnel2.server.TunnelServer
import tunnel2.server.interceptor.SimpleRequestInterceptor
import tunnel2.t2cli.*
import java.io.FileReader

class T2s : AbstractApp<RunOptions>() {

    override fun newRunOptions(): RunOptions {
        return RunOptions()
    }

    @Throws(Exception::class)
    override fun runApp(runOptions: RunOptions) {
        if (runOptions.configFile.isNotEmpty()) runAppByCfg(runOptions.configFile)
        else runAppByArgs(runOptions)
        Thread.currentThread().join()
    }

    @Throws(Exception::class)
    private fun runAppByCfg(cfgFile: String) {
        val globalCfg = Yaml().loadAs(FileReader(cfgFile), Map::class.java)

        // ================================== logCfg ================================== //
        val logCfg = globalCfg.getMap("log")
        LoggerFactory.configConsole(Level.OFF, names = *LoggerFactory.thirdLibs).apply()
        // 配置控制台日志
        LoggerFactory.configConsole(
            level = LogLevel.valueOf(logCfg.getString("level", "OFF").toUpperCase()).level
        ).apply()
        // 配置文件日志
        LoggerFactory.configFile(
            level = LogLevel.valueOf(logCfg.getString("level", "OFF").toUpperCase()).level,
            file = logCfg.getString("file", "./logs/t2s.log"),
            maxFileSize = OptionConverter.toFileSize(logCfg.getString("max_file_size", "1MB"), 1),
            maxBackupIndex = logCfg.getInt("max_backup_index", 3)
        ).apply()

        // ================================== common ================================== //
        val bossThreads = globalCfg.getInt("boss_threads", -1)
        val workerThreads = globalCfg.getInt("worker_threads", -1)
        val authToken = globalCfg.getString("auth_token", "")
        val allowPorts = globalCfg.getString("allow_ports", "1024-65535")
        val bindAddr = globalCfg.getString("bind_addr", "0.0.0.0")
        val bindPort = globalCfg.getInt("bind_port", 5000)

        // ================================== sslCfg ================================== //
        val sslCfg = globalCfg.getMap("ssl")
        val sslEnable = sslCfg.getBoolean("enable", false)
        val sslBindAddr = sslCfg.getString("bind_addr", "0.0.0.0")
        val sslBindPort = sslCfg.getInt("bind_port", 5001)
        var sslContext: SslContext? = null
        if (sslCfg.isNotEmpty() && sslEnable) {
            sslContext = SslContexts.forServer(
                sslCfg.getString("jks", ""),
                sslCfg.getString("storepass", ""),
                sslCfg.getString("keypass", "")
            )
        }

        // ================================== httpCfg ================================== //
        val httpCfg = globalCfg.getMap("http")
        val httpEnable = httpCfg.getBoolean("enable", false)
        val httpBindAddr = httpCfg.getString("bind_addr", "0.0.0.0")
        val httpBindPort = httpCfg.getInt("bind_port", 5080)

        // ================================== https ================================== //
        val httpsCfg = globalCfg.getMap("https")
        val httpsEnable = httpsCfg.getBoolean("enable", false)
        val httpsBindAddr = httpsCfg.getString("bind_addr", "0.0.0.0")
        val httpsBindPort = httpsCfg.getInt("bind_port", 5443)
        var httpsContext: SslContext? = null
        if (httpsCfg.isNotEmpty() && httpsEnable) {
            httpsContext = SslContexts.forServer(
                httpsCfg.getString("jks", ""),
                httpsCfg.getString("storepass", ""),
                httpsCfg.getString("keypass", "")
            )
        }

        // ==============================================================================
        val simpleRequestInterceptor = SimpleRequestInterceptor(authToken, allowPorts)

        TunnelServer(
            bossThreads = bossThreads,
            workerThreads = workerThreads,
            tunnelRequestInterceptor = simpleRequestInterceptor,
            bindAddr = if (bindAddr.isEmpty()) null else bindAddr,
            bindPort = bindPort,
            sslEnable = sslEnable,
            sslContext = sslContext,
            sslBindAddr = if (sslBindAddr.isEmpty()) null else sslBindAddr,
            sslBindPort = sslBindPort,
            httpEnable = httpEnable,
            httpBindAddr = if (httpBindAddr.isEmpty()) null else httpBindAddr,
            httpBindPort = httpBindPort,
            httpRequestInterceptor = simpleRequestInterceptor,
            httpsEnable = httpsEnable,
            httpsContext = httpsContext,
            httpsBindAddr = if (httpsBindAddr.isEmpty()) null else httpsBindAddr,
            httpsBindPort = httpsBindPort,
            httpsRequestInterceptor = simpleRequestInterceptor
        ).start()
    }

    @Throws(Exception::class)
    private fun runAppByArgs(runOptions: RunOptions) {

        LoggerFactory.configConsole(Level.OFF, names = *LoggerFactory.thirdLibs).apply()
        // 配置控制台日志
        LoggerFactory.configConsole(
            level = runOptions.logLevel.level
        ).apply()
        // 配置文件日志
        LoggerFactory.configFile(
            level = runOptions.logLevel.level,
            file = runOptions.logFile,
            maxFileSize = OptionConverter.toFileSize(runOptions.logMaxFileSize, 1),
            maxBackupIndex = runOptions.logMaxBackupIndex
        ).apply()


        val simpleRequestInterceptor = SimpleRequestInterceptor(runOptions.authToken, runOptions.allowPorts)

        var sslContext: SslContext? = null
        if (runOptions.sslEnable) {
            sslContext = SslContexts.forServer(
                runOptions.sslJks,
                runOptions.sslStorepass,
                runOptions.sslKeypass
            )
        }

        var httpsContext: SslContext? = null
        if (runOptions.httpsEnable) {
            httpsContext = SslContexts.forServer(
                runOptions.httpsJks,
                runOptions.httpsStorepass,
                runOptions.httpsKeypass
            )
        }

        TunnelServer(
            bossThreads = runOptions.bossThreads,
            workerThreads = runOptions.workerThreads,
            tunnelRequestInterceptor = simpleRequestInterceptor,
            bindAddr = if (runOptions.bindAddr.isEmpty()) null else runOptions.bindAddr,
            bindPort = runOptions.bindPort,
            sslEnable = runOptions.sslEnable,
            sslContext = sslContext,
            sslBindAddr = if (runOptions.sslBindAddr.isEmpty()) null else runOptions.sslBindAddr,
            sslBindPort = runOptions.sslBindPort,
            httpEnable = runOptions.httpEnable,
            httpBindAddr = if (runOptions.httpBindAddr.isEmpty()) null else runOptions.httpBindAddr,
            httpBindPort = runOptions.httpBindPort,
            httpRequestInterceptor = simpleRequestInterceptor,
            httpsEnable = runOptions.httpsEnable,
            httpsContext = httpsContext,
            httpsBindAddr = if (runOptions.httpsBindAddr.isEmpty()) null else runOptions.httpsBindAddr,
            httpsBindPort = runOptions.httpsBindPort,
            httpsRequestInterceptor = simpleRequestInterceptor
        ).start()
    }

}
