package clientcli;

import keep.RunOptions;
import com.tuuzed.tunnel.client.TunnelClient;
import com.tuuzed.tunnel.client.TunnelClientDescriptor;
import com.tuuzed.tunnel.client.TunnelClientListener;
import com.tuuzed.tunnel.common.log4j.Log4jInitializer;
import com.tuuzed.tunnel.common.proto.Proto;
import com.tuuzed.tunnel.common.proto.ProtoRequest;
import com.tuuzed.tunnel.common.util.SslContexts;
import com.tuuzed.tunnel.commoncli.AbstractApp;
import com.tuuzed.tunnel.commoncli.LogLevel;
import com.tuuzed.tunnel.commoncli.Maps;
import io.netty.handler.ssl.SslContext;
import org.apache.log4j.Level;
import org.apache.log4j.helpers.OptionConverter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import java.io.FileReader;
import java.util.List;
import java.util.Map;

@SuppressWarnings("Duplicates")
public final class TunnelClientApp extends AbstractApp<RunOptions> {
    private static final Logger logger = LoggerFactory.getLogger(TunnelClientApp.class);

    @NotNull
    @Override
    public RunOptions newRunOptions() {
        return new RunOptions();
    }

    @NotNull
    private final TunnelClientListener tunnelClientListener = new TunnelClientListener() {
        @Override
        public void onConnecting(@NotNull TunnelClientDescriptor descriptor, boolean reconnect) {
            logger.debug("onDisconnect: {}", descriptor.toString());
        }

        @Override
        public void onConnected(@NotNull TunnelClientDescriptor descriptor) {
            logger.info("onConnected: {}", descriptor.toString());
        }

        @Override
        public void onDisconnect(@NotNull TunnelClientDescriptor descriptor, boolean fatal) {
            logger.info("onDisconnect: {}", descriptor.toString());
        }
    };

    private TunnelClient createTunnelClient(int workerThreads) {
        return TunnelClient.builder()
            .setWorkerThreads(workerThreads)
            .setAutoReconnect(true)
            .setListener(tunnelClientListener)
            .build();
    }

    @Override
    public void runApp(@NotNull RunOptions runOptions) throws Exception {
        if (runOptions.configFile.length() != 0) {
            runAppAtCfg(runOptions.configFile);
        } else {
            runAppAtArgs(runOptions);
        }
        Thread.currentThread().join();
    }

    private void runAppAtCfg(@NotNull String cfgFile) throws Exception {
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
            .setFile(Maps.getString(logCfg, "file", "./logs/tunnel-client.log"))
            .setMaxFileSize(OptionConverter.toFileSize(Maps.getString(logCfg, "max_file_size", "1MB"), 1))
            .setMaxBackupIndex(Maps.getInt(logCfg, "max_backup_index", 3))
            .initialize();

        // ================================== common ================================== //
        final String serverAddr = Maps.getString(globalCfg, "server_addr", "0.0.0.0");
        final int serverPort = Maps.getInt(globalCfg, "server_addr", 5000);
        final String token = Maps.getString(globalCfg, "token", "");
        final int workerThreads = Maps.getInt(globalCfg, "worker_threads", -1);

        // ================================== sslCfg ================================== //
        @Nullable SslContext sslContext = null;
        int sslServerPort = serverPort;
        final Map sslCfg = Maps.getMap(globalCfg, "ssl");

        if (!sslCfg.isEmpty()) {
            sslContext = SslContexts.forClient(
                Maps.getString(sslCfg, "jks", ""),
                Maps.getString(sslCfg, "storepass", "")
            );
            sslServerPort = Maps.getInt(sslCfg, "server_port", 5001);
        }


        // ================================== tunnelsCfg ================================== //

        final List<Map> tunnelsCfg = Maps.getListMap(globalCfg, "tunnels");
        for (Map tunnelCfg : tunnelsCfg) {
            final Proto proto = Proto.valueOf(
                Maps.getString(tunnelCfg, "proto", "unknown").toUpperCase()
            );
            final boolean enableSsl = Maps.getBoolean(tunnelCfg, "enable_ssl", false);
            final String localAddr = Maps.getString(tunnelCfg, "local_addr", "");
            final int localPort = Maps.getInt(tunnelCfg, "local_port", 0);

            ProtoRequest protoRequest = null;
            switch (proto) {
                case TCP:
                    final int remotePort = Maps.getInt(tunnelCfg, "remote_port", 0);
                    protoRequest = ProtoRequest.tcpBuilder(remotePort)
                        .setLocalAddr(localAddr)
                        .setLocalPort(localPort)
                        .setToken(token)
                        .build();
                    break;
                case HTTP:
                case HTTPS:
                    final String vhost = Maps.getString(tunnelCfg, "vhost", "");
                    @SuppressWarnings("unchecked")
                    Map<String, String> rewriteHeaders = Maps.getMap(tunnelCfg, "rewrite_headers");
                    @SuppressWarnings("unchecked")
                    Map<String, String> writeHeaders = Maps.getMap(tunnelCfg, "write_headers");
                    Map auth = Maps.getMap(tunnelCfg, "auth");
                    boolean authEnable = Maps.getBoolean(auth, "enable", false);
                    String authRealm = Maps.getString(auth, "realm", ".");
                    String authUsername = Maps.getString(auth, "username", "");
                    String authPassword = Maps.getString(auth, "password", "");
                    ProtoRequest.Builder builder = null;
                    switch (proto) {
                        case HTTP:
                            builder = ProtoRequest.httpBuilder(vhost);
                            break;
                        case HTTPS:
                            builder = ProtoRequest.httpsBuilder(vhost);
                            break;
                    }
                    protoRequest = builder
                        .setLocalAddr(localAddr)
                        .setLocalPort(localPort)
                        .setToken(token)
                        .setRewriteHeaders(rewriteHeaders)
                        .setWriteHeaders(writeHeaders)
                        .setBasicAuth(authEnable, authRealm)
                        .setBasicAuthAccount(authUsername, authPassword)
                        .build();
                    break;
                default:
                    break;
            }
            TunnelClient tunnelClient = createTunnelClient(workerThreads);
            if (protoRequest != null) {
                tunnelClient.connect(
                    serverAddr,
                    (enableSsl) ? sslServerPort : serverPort,
                    protoRequest,
                    (enableSsl) ? sslContext : null
                );
            }
        }
    }

    private void runAppAtArgs(@NotNull RunOptions runOptions) throws Exception {

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

        TunnelClient tunnelClient = createTunnelClient(runOptions.workerThreads);
        ProtoRequest protoRequest = null;
        switch (runOptions.proto) {
            case TCP:
                protoRequest = ProtoRequest.tcpBuilder(runOptions.remotePort)
                    .setLocalAddr(runOptions.localAddr)
                    .setLocalPort(runOptions.localPort)
                    .setToken(runOptions.token)
                    .build();

                break;
            case HTTP:
            case HTTPS:
                final String vhost = runOptions.vhost;
                ProtoRequest.Builder builder = null;
                switch (runOptions.proto) {
                    case HTTP:
                        builder = ProtoRequest.httpBuilder(vhost);
                        break;
                    case HTTPS:
                        builder = ProtoRequest.httpsBuilder(vhost);
                        break;
                }
                protoRequest = builder
                    .setLocalAddr(runOptions.localAddr)
                    .setLocalPort(runOptions.localPort)
                    .setToken(runOptions.token)
                    .setRewriteHeaders(runOptions.setHeaders)
                    .setWriteHeaders(runOptions.addHeaders)
                    .setBasicAuth(runOptions.authEnable, runOptions.authRealm)
                    .setBasicAuthAccount(runOptions.authUsername, runOptions.authPassword)
                    .build();
                break;
            default:
                break;
        }
        SslContext sslContext = null;
        if (runOptions.sslEnable) {
            sslContext = SslContexts.forClient(runOptions.sslJks, runOptions.sslStorepass);
        }
        if (protoRequest != null) {
            tunnelClient.connect(runOptions.serverAddr, runOptions.serverPort, protoRequest, sslContext);
        }
    }


}
