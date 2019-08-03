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
import org.yaml.snakeyaml.Yaml;

import java.io.FileReader;
import java.util.Map;

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
        final String bindAddr = CfgUtils.getString(globalOptions, "bind_addr", "0.0.0.0");
        final int bindPort = CfgUtils.getInt(globalOptions, "bind_port", 5000);
        final String token = CfgUtils.getString(globalOptions, "token", "");
        final String allowPorts = CfgUtils.getString(globalOptions, "allow_ports", "1024-65535");
        final int bossThreads = CfgUtils.getInt(globalOptions, "boss_threads", -1);
        final int workerThreads = CfgUtils.getInt(globalOptions, "worker_threads", -1);
        // http
        final Map httpOptions = CfgUtils.getMap(globalOptions, "http");
        final String httpBindAddr = CfgUtils.getString(httpOptions, "bind_addr", "0.0.0.0");
        final int httpBindPort = CfgUtils.getInt(httpOptions, "bind_port", 5080);


        final ProtoRequestInterceptor protoRequestInterceptor = new DefaultProtoRequestInterceptor(
            token,
            allowPorts
        );
        final HttpRequestInterceptor httpRequestInterceptor = new DefaultHttpRequestInterceptor();

        final TunnelServerBuilder builder = TunnelServer.builder()
            .setBindAddr(bindAddr.length() == 0 ? null : bindAddr)
            .setBindPort(bindPort)
            .setBossThreads(bossThreads)
            .setWorkerThreads(workerThreads)
            .setProtoRequestInterceptor(protoRequestInterceptor)
            .setHttpRequestInterceptor(httpRequestInterceptor)
            // http
            .setHttpBindAddr(httpBindAddr)
            .setHttpBindPort(httpBindPort);

        // ssl
        final Map sslOptions = CfgUtils.getMap(globalOptions, "ssl");
        if (!sslOptions.isEmpty()) {
            SslContext context = SslContexts.forServer(
                CfgUtils.getString(sslOptions, "jks", ""),
                CfgUtils.getString(sslOptions, "storepass", ""),
                CfgUtils.getString(sslOptions, "keypass", "")
            );
            final int sslBindPort = CfgUtils.getInt(sslOptions, "bind_port", 5000);
            builder.enableSsl(context, sslBindPort);
        }
        builder.build().start();

    }

    private void runAppAtArgs(@NotNull final RunOptions runOptions) throws Exception {
        final ProtoRequestInterceptor protoRequestInterceptor = new DefaultProtoRequestInterceptor(
            runOptions.token,
            runOptions.allowPorts
        );
        final HttpRequestInterceptor httpRequestInterceptor = new DefaultHttpRequestInterceptor();
        TunnelServerBuilder builder = TunnelServer.builder()
            .setBindAddr(runOptions.bindAddr)
            .setBindPort(runOptions.bindPort)
            .setHttpBindAddr(runOptions.httpBindAddr)
            .setHttpBindPort(runOptions.httpBindPort)
            .setBossThreads(runOptions.bossThreads)
            .setWorkerThreads(runOptions.workerThreads)
            .setProtoRequestInterceptor(protoRequestInterceptor)
            .setHttpRequestInterceptor(httpRequestInterceptor);
        if (runOptions.ssl) {
            SslContext context = SslContexts.forServer(
                runOptions.sslJks,
                runOptions.sslStorepass,
                runOptions.sslKeypass
            );
            builder.enableSsl(context, runOptions.sslBindAddr, runOptions.sslBindPort);
        }
        builder.build().start();
    }


}
