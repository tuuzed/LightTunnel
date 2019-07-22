package com.tuuzed.tunnel.cli.client;

import com.esotericsoftware.yamlbeans.YamlReader;
import com.tuuzed.tunnel.cli.common.AbstractApp;
import com.tuuzed.tunnel.client.TunnelClient;
import com.tuuzed.tunnel.common.logging.Logger;
import com.tuuzed.tunnel.common.logging.LoggerFactory;
import com.tuuzed.tunnel.common.protocol.OpenTunnelRequest;
import com.tuuzed.tunnel.common.ssl.SslContexts;
import io.netty.handler.ssl.SslContext;
import org.jetbrains.annotations.NotNull;

import java.io.FileReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TunnelClientApp extends AbstractApp<RunOptions> {
    private static final Logger logger = LoggerFactory.getLogger(TunnelClientApp.class);

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
            System.in.read();
        } catch (Exception e) {
            logger.error("runApp Error", e);
        }
    }

    private void runAppAtCfg(@NotNull RunOptions runOptions) throws Exception {
        YamlReader reader = new YamlReader(new FileReader(runOptions.configFile));
        Map cfgOptions = (Map) reader.read();
        logger.info("cfgOptions: {}", cfgOptions);

        final String serverAddr = cfgOptions.get("server_addr").toString();

        int serverPort;
        try {
            serverPort = Integer.parseInt(cfgOptions.get("server_port").toString());
        } catch (Exception e) {
            serverPort = 5000;
        }

        final String token = cfgOptions.get("token").toString();

        final Map<String, String> arguments = new HashMap<>();
        arguments.put("token", token);
        @SuppressWarnings("unchecked") final List<Map> tunnels = (List) cfgOptions.get("tunnels");

        int workerThreads;
        try {
            workerThreads = Integer.parseInt(cfgOptions.get("worker_threads").toString());
        } catch (Exception e) {
            workerThreads = -1;
        }

        final TunnelClient tunnelClient = new TunnelClient.Builder()
                .setWorkerThreads(workerThreads)
                .setAutoReconnect(true)
                .build();

        SslContext context = null;
        int sslServerPort = serverPort;
        final Map sslOptions = (Map) cfgOptions.get("ssl");
        if (sslOptions != null) {
            context = SslContexts.forClient(
                    (String) sslOptions.get("jks"),
                    (String) sslOptions.get("storepass")
            );
            sslServerPort = Integer.parseInt(sslOptions.get("server_port").toString());
        }
        for (Map tunnel : tunnels) {
            final String localAddr = tunnel.get("local_addr").toString();
            final int localPort = Integer.parseInt(tunnel.get("local_port").toString());
            final int remotePort = Integer.parseInt(tunnel.get("remote_port").toString());
            final boolean enableSsl = Boolean.parseBoolean(tunnel.get("enable_ssl").toString());
            OpenTunnelRequest request = new OpenTunnelRequest(OpenTunnelRequest.TYPE_TCP,
                    localAddr,
                    localPort,
                    remotePort,
                    arguments
            );
            tunnelClient.connect(
                    serverAddr,
                    (enableSsl) ? sslServerPort : serverPort,
                    request,
                    (enableSsl) ? context : null
            );
        }
    }

    private void runAppAtArgs(@NotNull RunOptions runOptions) throws Exception {
        Map<String, String> arguments = new HashMap<>();
        arguments.put("token", runOptions.token);
        TunnelClient tunnelClient = new TunnelClient.Builder()
                .setWorkerThreads(runOptions.workerThreads)
                .setAutoReconnect(true)
                .build();
        OpenTunnelRequest request = new OpenTunnelRequest(OpenTunnelRequest.TYPE_TCP,
                runOptions.localAddr,
                runOptions.localPort,
                runOptions.remotePort,
                arguments
        );
        SslContext context = null;
        if (runOptions.ssl) {
            context = SslContexts.forClient(runOptions.sslJks, runOptions.sslStorepass);
        }
        tunnelClient.connect(runOptions.serverAddr, runOptions.serverPort, request, context);
    }


}
