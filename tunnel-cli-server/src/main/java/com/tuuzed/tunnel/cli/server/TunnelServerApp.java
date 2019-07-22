package com.tuuzed.tunnel.cli.server;

import com.esotericsoftware.yamlbeans.YamlReader;
import com.tuuzed.tunnel.cli.common.AbstractApp;
import com.tuuzed.tunnel.common.logging.Logger;
import com.tuuzed.tunnel.common.logging.LoggerFactory;
import com.tuuzed.tunnel.common.ssl.SslContexts;
import com.tuuzed.tunnel.server.TunnelServer;
import io.netty.handler.ssl.SslContext;
import org.jetbrains.annotations.NotNull;

import java.io.FileReader;
import java.util.Map;

public class TunnelServerApp extends AbstractApp<RunOptions> {
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
        YamlReader reader = new YamlReader(new FileReader(runOptions.configFile));
        Map cfgOptions = (Map) reader.read();
        logger.info("cfgOptions: {}", cfgOptions);
        final String bindAddr = (String) cfgOptions.get("bind_addr");

        int bindPort;
        try {
            bindPort = Integer.parseInt(cfgOptions.get("bind_port").toString());
        } catch (Exception e) {
            bindPort = 5000;
        }

        final String token = (String) cfgOptions.get("token");
        final String allowPorts = (String) cfgOptions.get("allow_ports");

        int bossThreads;
        try {
            bossThreads = Integer.parseInt(cfgOptions.get("boss_threads").toString());
        } catch (Exception e) {
            bossThreads = -1;
        }

        int workerThreads;
        try {
            workerThreads = Integer.parseInt(cfgOptions.get("worker_threads").toString());
        } catch (Exception e) {
            workerThreads = -1;
        }

        final TunnelServer.Builder builder = new TunnelServer.Builder()
                .setBindAddr(bindAddr.length() == 0 ? null : bindAddr)
                .setBindPort(bindPort)
                .setBossThreads(bossThreads)
                .setWorkerThreads(workerThreads)
                .setInterceptor(new OpenTunnelRequestInterceptorImpl(token, allowPorts));
        final Map sslOptions = (Map) cfgOptions.get("ssl");
        if (sslOptions != null) {
            SslContext context = SslContexts.forServer(
                    (String) sslOptions.get("jks"),
                    (String) sslOptions.get("storepass"),
                    (String) sslOptions.get("keypass")
            );
            int sslBindPort;
            try {
                sslBindPort = Integer.parseInt(sslOptions.get("bind_port").toString());
            } catch (Exception e) {
                sslBindPort = 5001;
            }
            builder.enableSsl(context, sslBindPort);
        }
        builder.build().start();

    }

    private void runAppAtArgs(@NotNull final RunOptions runOptions) throws Exception {

        TunnelServer.Builder builder = new TunnelServer.Builder()
                .setBindAddr(runOptions.bindAddr)
                .setBindPort(runOptions.bindPort)
                .setBossThreads(runOptions.bossThreads)
                .setWorkerThreads(runOptions.workerThreads)
                .setInterceptor(new OpenTunnelRequestInterceptorImpl(runOptions.token, runOptions.allowPorts));
        if (runOptions.ssl) {
            SslContext context = SslContexts.forServer(
                    runOptions.sslJks,
                    runOptions.sslStorepass,
                    runOptions.sslKeypass
            );
            builder.enableSsl(context, runOptions.sslBindPort);
        }
        builder.build().start();
    }

}
