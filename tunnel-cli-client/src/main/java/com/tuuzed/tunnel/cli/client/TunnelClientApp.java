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
import org.kohsuke.args4j.Option;

import java.io.FileReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TunnelClientApp extends AbstractApp<TunnelClientApp.RunOptions> {
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

    static class RunOptions {

        @Option(name = "-c", aliases = {"--configFile"}, help = true, metaVar = "<string>", usage = "配置文件，当设置了配置文件时优先使用配置文件配置项")
        public String configFile = "";

        @Option(name = "-s", aliases = {"--serverAddr"}, help = true, metaVar = "<string>", usage = "服务器地址")
        public String serverAddr = "127.0.0.1";

        @Option(name = "-p", aliases = {"--serverPort"}, help = true, metaVar = "<int>", usage = "服务器端口")
        public int serverPort = 5000;

        @Option(name = "-la", aliases = {"--localAddr"}, help = true, metaVar = "<string>", usage = "内网地址")
        public String localAddr = "127.0.0.1";

        @Option(name = "-lp", aliases = {"--localPort"}, help = true, metaVar = "<int>", usage = "内网端口")
        public int localPort = 80;

        @Option(name = "-rp", aliases = {"--remotePort"}, help = true, metaVar = "<int>", usage = "映射外网端口")
        public int remotePort = 10080;

        @Option(name = "-t", aliases = {"--token"}, help = true, metaVar = "<int>", usage = "令牌")
        public String token = "";

        @Option(name = "-workerThreads", aliases = {"--workerThreads"}, help = true, metaVar = "<int>", usage = "Worker线程数量")
        public int workerThreads = -1;

        // SSL
        @Option(name = "-ssl", aliases = {"--ssl"}, help = true, metaVar = "<boolean>", usage = "是否启用SSL")
        public boolean ssl = false;

        @Option(name = "-ssl-jks", aliases = {"--ssl-jks"}, help = true, metaVar = "<string>", usage = "jks签名文件")
        public String sslJks = "";

        @Option(name = "-ssl-storepass", aliases = {"--ssl-storepass"}, help = true, metaVar = "<string>", usage = "jks签名文件Store密码")
        public String sslStorepass = "";


    }

}
