package com.tuuzed.tunnel.cli.server;

import com.tuuzed.tunnel.cli.common.AbstractApp;
import com.tuuzed.tunnel.cli.common.CfgUtils;
import com.tuuzed.tunnel.common.logging.Logger;
import com.tuuzed.tunnel.common.logging.LoggerFactory;
import com.tuuzed.tunnel.common.proto.ProtoRequestInterceptor;
import com.tuuzed.tunnel.common.util.SslContexts;
import com.tuuzed.tunnel.server.TunnelServer;
import com.tuuzed.tunnel.server.TunnelServerBuilder;
import com.tuuzed.tunnel.server.http.HttpRequestInterceptor;
import io.netty.handler.ssl.SslContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.yaml.snakeyaml.Yaml;

import java.io.FileReader;
import java.util.Map;

@SuppressWarnings("Duplicates")
public final class TunnelServerApp extends AbstractApp<RunOptions> {
    private static final Logger logger = LoggerFactory.getLogger(TunnelServerApp.class);

    @NotNull
    @Override
    public RunOptions newRunOptions() {
        return new RunOptions();
    }

    @Override
    public void runApp(@NotNull RunOptions runOptions) {
        try {
            if (runOptions.configFile.length() != 0) {
                runAppAtCfg(runOptions);
            } else {
                runAppAtArgs(runOptions);
            }
        } catch (Exception e) {
            logger.error("runApp Error", e);
        }
    }

    private void runAppAtCfg(@NotNull final RunOptions runOptions) throws Exception {
        final Map globalOptions = new Yaml().loadAs(new FileReader(runOptions.configFile), Map.class);

        // common
        final int bossThreads = CfgUtils.getInt(globalOptions, "boss_threads", -1);
        final int workerThreads = CfgUtils.getInt(globalOptions, "worker_threads", -1);
        //
        final String token = CfgUtils.getString(globalOptions, "token", "");
        final String allowPorts = CfgUtils.getString(globalOptions, "allow_ports", "1024-65535");


        // auth
        final String bindAddr = CfgUtils.getString(globalOptions, "bind_addr", "0.0.0.0");
        final int bindPort = CfgUtils.getInt(globalOptions, "bind_port", 5000);
        // ssl auth
        final Map sslOptions = CfgUtils.getMap(globalOptions, "ssl");
        final boolean sslEnable = CfgUtils.getBoolean(sslOptions, "enable", false);
        final String sslBindAddr = CfgUtils.getString(sslOptions, "bind_addr", "0.0.0.0");
        final int sslBindPort = CfgUtils.getInt(sslOptions, "bind_port", 5001);
        @Nullable SslContext sslContext = null;
        if (!sslOptions.isEmpty() && sslEnable) {
            sslContext = SslContexts.forServer(
                CfgUtils.getString(sslOptions, "jks", ""),
                CfgUtils.getString(sslOptions, "storepass", ""),
                CfgUtils.getString(sslOptions, "keypass", "")
            );
        }
        // http
        final Map httpOptions = CfgUtils.getMap(globalOptions, "http");
        final boolean httpEnable = CfgUtils.getBoolean(httpOptions, "enable", false);
        final String httpBindAddr = CfgUtils.getString(httpOptions, "bind_addr", "0.0.0.0");
        final int httpBindPort = CfgUtils.getInt(httpOptions, "bind_port", 5080);
        // https
        final Map httpsOptions = CfgUtils.getMap(globalOptions, "https");
        final boolean httpsEnable = CfgUtils.getBoolean(httpsOptions, "enable", false);
        final String httpsBindAddr = CfgUtils.getString(httpsOptions, "bind_addr", "0.0.0.0");
        final int httpsBindPort = CfgUtils.getInt(httpsOptions, "bind_port", 5443);
        @Nullable SslContext httpsContext = null;
        if (!sslOptions.isEmpty() && sslEnable) {
            httpsContext = SslContexts.forServer(
                CfgUtils.getString(httpsOptions, "jks", ""),
                CfgUtils.getString(httpsOptions, "storepass", ""),
                CfgUtils.getString(httpsOptions, "keypass", "")
            );
        }


        final ProtoRequestInterceptor protoRequestInterceptor = new DefaultProtoRequestInterceptor(
            token,
            allowPorts
        );
        final HttpRequestInterceptor httpRequestInterceptor = new DefaultHttpRequestInterceptor();

        final TunnelServerBuilder builder = TunnelServer.builder()
            .setBossThreads(bossThreads)
            .setWorkerThreads(workerThreads)
            //
            .setProtoRequestInterceptor(protoRequestInterceptor)
            //
            .setBindAddr(bindAddr.length() == 0 ? null : bindAddr)
            .setBindPort(bindPort)
            // ssl
            .setSslEnable(sslEnable)
            .setSslContext(sslContext)
            .setSslBindAddr(sslBindAddr.length() == 0 ? null : sslBindAddr)
            .setSslBindPort(sslBindPort)
            //
            // http
            .setHttpEnable(httpEnable)
            .setHttpBindAddr(httpBindAddr.length() == 0 ? null : httpBindAddr)
            .setHttpBindPort(httpBindPort)
            .setHttpRequestInterceptor(httpRequestInterceptor)
            // https
            .setHttpsEnable(httpsEnable)
            .setHttpsContext(httpsContext)
            .setHttpsBindAddr(httpsBindAddr.length() == 0 ? null : httpsBindAddr)
            .setHttpsBindPort(httpsBindPort)
            .setHttpsRequestInterceptor(httpRequestInterceptor);

        builder.build().start();
    }

    private void runAppAtArgs(@NotNull final RunOptions runOptions) throws Exception {
        final ProtoRequestInterceptor protoRequestInterceptor = new DefaultProtoRequestInterceptor(
            runOptions.token,
            runOptions.allowPorts
        );
        final HttpRequestInterceptor httpRequestInterceptor = new DefaultHttpRequestInterceptor();

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

        final TunnelServerBuilder builder = TunnelServer.builder()
            .setBossThreads(runOptions.bossThreads)
            .setWorkerThreads(runOptions.workerThreads)
            //
            .setProtoRequestInterceptor(protoRequestInterceptor)
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
            .setHttpRequestInterceptor(httpRequestInterceptor)
            // https
            .setHttpsEnable(runOptions.httpsEnable)
            .setHttpsContext(httpsContext)
            .setHttpsBindAddr(runOptions.httpsBindAddr.length() == 0 ? null : runOptions.httpsBindAddr)
            .setHttpsBindPort(runOptions.httpsBindPort)
            .setHttpsRequestInterceptor(httpRequestInterceptor);

        builder.build().start();
    }


}
