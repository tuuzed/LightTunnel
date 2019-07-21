package com.tuuzed.tunnel.cli.server;

import com.esotericsoftware.yamlbeans.YamlReader;
import com.tuuzed.tunnel.cli.common.AbstractApp;
import com.tuuzed.tunnel.common.logging.Logger;
import com.tuuzed.tunnel.common.logging.LoggerFactory;
import com.tuuzed.tunnel.common.protocol.OpenTunnelRequest;
import com.tuuzed.tunnel.common.protocol.TunnelProtocolException;
import com.tuuzed.tunnel.common.ssl.SslContexts;
import com.tuuzed.tunnel.common.util.PortUtils;
import com.tuuzed.tunnel.server.TunnelServer;
import io.netty.handler.ssl.SslContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.kohsuke.args4j.Option;

import java.io.FileReader;
import java.util.Map;

public class TunnelServerApp extends AbstractApp<TunnelServerApp.RunOptions> {
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
        final int bindPort = Integer.parseInt(cfgOptions.get("bind_port").toString());
        final String token = (String) cfgOptions.get("token");
        final String allowPorts = (String) cfgOptions.get("allow_ports");
        TunnelServer.Builder builder = new TunnelServer.Builder()
                .setBindAddr(bindAddr.length() == 0 ? null : bindAddr)
                .setBindPort(bindPort)
                .setInterceptor(new OpenTunnelRequestInterceptor(token, allowPorts));
        final Map sslOptions = (Map) cfgOptions.get("ssl");
        if (sslOptions != null) {
            SslContext context = SslContexts.forServer(
                    (String) sslOptions.get("jks"),
                    (String) sslOptions.get("storepass"),
                    (String) sslOptions.get("keypass")
            );
            builder.enableSsl(context, Integer.parseInt(sslOptions.get("bind_port").toString()));
        }
        builder.build().start();

    }

    private void runAppAtArgs(@NotNull final RunOptions runOptions) throws Exception {
        final String bindAddr = runOptions.bindAddr;
        final int bindPort = runOptions.bindPort;
        final String token = runOptions.token;
        final String allowPorts = runOptions.allowPorts;


        TunnelServer.Builder builder = new TunnelServer.Builder()
                .setBindAddr(bindAddr)
                .setBindPort(bindPort)
                .setInterceptor(new OpenTunnelRequestInterceptor(token, allowPorts));
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

    private static class OpenTunnelRequestInterceptor implements com.tuuzed.tunnel.common.protocol.OpenTunnelRequestInterceptor {
        @NotNull
        private final String token;
        @Nullable
        private final String portRule;

        public OpenTunnelRequestInterceptor(@NotNull String token, @Nullable String portRule) {
            this.portRule = portRule;
            this.token = token;
        }

        @NotNull
        @Override
        public OpenTunnelRequest proceed(@NotNull OpenTunnelRequest request) throws TunnelProtocolException {
            String token = request.arguments.get("token");
            if (!this.token.equals(token)) {
                throw new TunnelProtocolException(String.format("request(%s), Bad Token(%s)", request.toString(), token));
            }
            if (portRule != null && !PortUtils.inPortRule(portRule, request.remotePort)) {
                throw new TunnelProtocolException(String.format("\"request(%s), remotePort(%s) Not allowed to use.", request.toString(), request.remotePort));
            }
            return request;
        }
    }

    static class RunOptions {
        @Option(name = "-c", aliases = {"--configFile"}, help = true, metaVar = "<string>", usage = "配置文件，当设置了配置文件时优先使用配置文件配置项")
        public String configFile = "";

        @Option(name = "-b", aliases = {"--bindAddr"}, help = true, metaVar = "<string>", usage = "绑定地址")
        public String bindAddr = "";

        @Option(name = "-p", aliases = {"--bindPort"}, help = true, metaVar = "<int>", usage = "绑定端口")
        public int bindPort = 5000;

        @Option(name = "-t", aliases = {"--token"}, help = true, metaVar = "<int>", usage = "令牌")
        public String token = "";

        @Option(name = "-a", aliases = {"--allowPorts"}, help = true, metaVar = "<string>", usage = "端口白名单，例如：10000-21000,30000,30001,30003")
        public String allowPorts = "1024-65534";

        // SSL
        @Option(name = "-ssl", aliases = {"--ssl"}, help = true, metaVar = "<boolean>", usage = "是否启用SSL")
        public boolean ssl = false;

        @Option(name = "-ssl-bindPort", aliases = {"--ssl-bindPort"}, help = true, metaVar = "<int>", usage = "SSL绑定端口")
        public int sslBindPort = 5001;

        @Option(name = "-ssl-jks", aliases = {"--ssl-jks"}, help = true, metaVar = "<string>", usage = "jks签名文件")
        public String sslJks = "";

        @Option(name = "-ssl-storepass", aliases = {"--ssl-storepass"}, help = true, metaVar = "<string>", usage = "jks签名文件Store密码")
        public String sslStorepass = "";

        @Option(name = "-ssl-keypass", aliases = {"--ssl-keypass"}, help = true, metaVar = "<string>", usage = "jks签名文件Key密码")
        public String sslKeypass = "";

    }
}
