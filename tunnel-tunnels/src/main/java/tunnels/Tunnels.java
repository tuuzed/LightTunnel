package tunnels;

import com.tuuzed.tunnel.interceptor.SimpleRequestInterceptor;
import com.tuuzed.tunnel.log4j.Log4jInitializer;
import com.tuuzed.tunnel.util.SslContexts;
import com.tuuzed.tunnelcli.AbstractApp;
import com.tuuzed.tunnelcli.LogLevel;
import com.tuuzed.tunnelcli.Maps;
import com.tuuzed.tunnel.server.TunnelServer;
import io.netty.handler.ssl.SslContext;
import org.apache.log4j.Level;
import org.apache.log4j.helpers.OptionConverter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.yaml.snakeyaml.Yaml;

import java.io.FileReader;
import java.util.Map;

@SuppressWarnings("Duplicates")
public final class Tunnels extends AbstractApp<RunOptions> {

    @NotNull
    @Override
    public RunOptions newRunOptions() {
        return new RunOptions();
    }

    @Override
    public void runApp(@NotNull RunOptions runOptions) throws Exception {
        if (runOptions.configFile.length() != 0) {
            runAppByCfg(runOptions.configFile);
        } else {
            runAppByArgs(runOptions);
        }
        Thread.currentThread().join();
    }

    private void runAppByCfg(@NotNull final String cfgFile) throws Exception {
        final Map globalCfg = new Yaml().loadAs(new FileReader(cfgFile), Map.class);

        // ================================== logCfg ================================== //
        final Map logCfg = Maps.getMap(globalCfg, "log");
        Log4jInitializer.initializeThirdLibrary(Level.OFF);
        // 设置控制台日志
        Log4jInitializer.builder()
            .setConsole(true)
            .setLevel(LogLevel.valueOf(Maps.getString(logCfg, "level", "OFF").toUpperCase()).level)
            .initialize();
        // 配置文件日志
        Log4jInitializer.builder()
            .setConsole(false)
            .setLevel(LogLevel.valueOf(Maps.getString(logCfg, "level", "OFF").toUpperCase()).level)
            .setFile(Maps.getString(logCfg, "file", "./logs/tunnel-server.log"))
            .setMaxFileSize(OptionConverter.toFileSize(Maps.getString(logCfg, "max_file_size", "1MB"), 1))
            .setMaxBackupIndex(Maps.getInt(logCfg, "max_backup_index", 3))
            .initialize();

        // ================================== common ================================== //
        final int bossThreads = Maps.getInt(globalCfg, "boss_threads", -1);
        final int workerThreads = Maps.getInt(globalCfg, "worker_threads", -1);
        final String token = Maps.getString(globalCfg, "token", "");
        final String allowPorts = Maps.getString(globalCfg, "allow_ports", "1024-65535");
        final String bindAddr = Maps.getString(globalCfg, "bind_addr", "0.0.0.0");
        final int bindPort = Maps.getInt(globalCfg, "bind_port", 5000);

        // ================================== sslCfg ================================== //
        final Map sslCfg = Maps.getMap(globalCfg, "ssl");
        final boolean sslEnable = Maps.getBoolean(sslCfg, "enable", false);
        final String sslBindAddr = Maps.getString(sslCfg, "bind_addr", "0.0.0.0");
        final int sslBindPort = Maps.getInt(sslCfg, "bind_port", 5001);
        @Nullable SslContext sslContext = null;
        if (!sslCfg.isEmpty() && sslEnable) {
            sslContext = SslContexts.forServer(
                Maps.getString(sslCfg, "jks", ""),
                Maps.getString(sslCfg, "storepass", ""),
                Maps.getString(sslCfg, "keypass", "")
            );
        }

        // ================================== httpCfg ================================== //
        final Map httpCfg = Maps.getMap(globalCfg, "http");
        final boolean httpEnable = Maps.getBoolean(httpCfg, "enable", false);
        final String httpBindAddr = Maps.getString(httpCfg, "bind_addr", "0.0.0.0");
        final int httpBindPort = Maps.getInt(httpCfg, "bind_port", 5080);

        // ================================== https ================================== //
        final Map httpsCfg = Maps.getMap(globalCfg, "https");
        final boolean httpsEnable = Maps.getBoolean(httpsCfg, "enable", false);
        final String httpsBindAddr = Maps.getString(httpsCfg, "bind_addr", "0.0.0.0");
        final int httpsBindPort = Maps.getInt(httpsCfg, "bind_port", 5443);
        @Nullable SslContext httpsContext = null;
        if (!httpsCfg.isEmpty() && httpsEnable) {
            httpsContext = SslContexts.forServer(
                Maps.getString(httpsCfg, "jks", ""),
                Maps.getString(httpsCfg, "storepass", ""),
                Maps.getString(httpsCfg, "keypass", "")
            );
        }

        // ==============================================================================
        final SimpleRequestInterceptor simpleRequestInterceptor = new SimpleRequestInterceptor(
            token,
            allowPorts
        );

        final TunnelServer.Builder builder = TunnelServer.builder()
            .setBossThreads(bossThreads)
            .setWorkerThreads(workerThreads)
            //
            .setProtoRequestInterceptor(simpleRequestInterceptor)
            //
            .setBindAddr(bindAddr.length() == 0 ? null : bindAddr)
            .setBindPort(bindPort)
            // ssl
            .setSslEnable(sslEnable)
            .setSslContext(sslContext)
            .setSslBindAddr(sslBindAddr.length() == 0 ? null : sslBindAddr)
            .setSslBindPort(sslBindPort)
            // http
            .setHttpEnable(httpEnable)
            .setHttpBindAddr(httpBindAddr.length() == 0 ? null : httpBindAddr)
            .setHttpBindPort(httpBindPort)
            .setHttpRequestInterceptor(simpleRequestInterceptor)
            // https
            .setHttpsEnable(httpsEnable)
            .setHttpsContext(httpsContext)
            .setHttpsBindAddr(httpsBindAddr.length() == 0 ? null : httpsBindAddr)
            .setHttpsBindPort(httpsBindPort)
            .setHttpsRequestInterceptor(simpleRequestInterceptor);

        builder.build().start();
    }

    private void runAppByArgs(@NotNull final RunOptions runOptions) throws Exception {

        Log4jInitializer.initializeThirdLibrary(Level.OFF);
        // 设置控制台日志
        Log4jInitializer.builder()
            .setConsole(true)
            .setLevel(runOptions.logLevel.level)
            .initialize();
        // 配置文件日志
        Log4jInitializer.builder()
            .setConsole(false)
            .setLevel(runOptions.logLevel.level)
            .setFile(runOptions.logFile)
            .setMaxFileSize(OptionConverter.toFileSize(runOptions.logMaxFileSize, 1))
            .setMaxBackupIndex(runOptions.logMaxBackupIndex)
            .initialize();

        final SimpleRequestInterceptor simpleRequestInterceptor = new SimpleRequestInterceptor(
            runOptions.token,
            runOptions.allowPorts
        );

        @Nullable SslContext sslContext = null;
        if (runOptions.sslEnable) {
            sslContext = SslContexts.forServer(
                runOptions.sslJks,
                runOptions.sslStorepass,
                runOptions.sslKeypass
            );
        }

        @Nullable SslContext httpsContext = null;
        if (runOptions.httpsEnable) {
            httpsContext = SslContexts.forServer(
                runOptions.httpsJks,
                runOptions.httpsStorepass,
                runOptions.httpsKeypass
            );
        }

        final TunnelServer.Builder builder = TunnelServer.builder()
            .setBossThreads(runOptions.bossThreads)
            .setWorkerThreads(runOptions.workerThreads)
            //
            .setProtoRequestInterceptor(simpleRequestInterceptor)
            //
            .setBindAddr(runOptions.bindAddr.length() == 0 ? null : runOptions.bindAddr)
            .setBindPort(runOptions.bindPort)
            // ssl
            .setSslEnable(runOptions.sslEnable)
            .setSslContext(sslContext)
            .setSslBindAddr(runOptions.sslBindAddr.length() == 0 ? null : runOptions.sslBindAddr)
            .setSslBindPort(runOptions.sslBindPort)
            //
            // http
            .setHttpEnable(runOptions.httpEnable)
            .setHttpBindAddr(runOptions.httpBindAddr.length() == 0 ? null : runOptions.httpBindAddr)
            .setHttpBindPort(runOptions.httpBindPort)
            .setHttpRequestInterceptor(simpleRequestInterceptor)
            // https
            .setHttpsEnable(runOptions.httpsEnable)
            .setHttpsContext(httpsContext)
            .setHttpsBindAddr(runOptions.httpsBindAddr.length() == 0 ? null : runOptions.httpsBindAddr)
            .setHttpsBindPort(runOptions.httpsBindPort)
            .setHttpsRequestInterceptor(simpleRequestInterceptor);

        builder.build().start();
    }

}
