package com.tuuzed.tunnel.cli;

import com.esotericsoftware.yamlbeans.YamlReader;
import com.tuuzed.tunnel.common.Interceptor;
import com.tuuzed.tunnel.common.TunnelProtocolException;
import com.tuuzed.tunnel.common.logging.Logger;
import com.tuuzed.tunnel.common.logging.LoggerFactory;
import com.tuuzed.tunnel.common.protocol.OpenTunnelRequest;
import com.tuuzed.tunnel.server.TunnelServer;
import org.jetbrains.annotations.NotNull;
import org.kohsuke.args4j.Option;

import java.io.FileReader;
import java.util.Map;

public class TunnelServerApp extends AbstractApp<TunnelServerApp.RunOptions> {
    private static final Logger logger = LoggerFactory.getLogger(TunnelServerApp.class);

    @NotNull
    @Override
    RunOptions newRunOptions() {
        return new RunOptions();
    }

    @Override
    void runApp(@NotNull RunOptions runOptions) {
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

    private void runAppAtCfg(@NotNull RunOptions runOptions) throws Exception {
        YamlReader reader = new YamlReader(new FileReader(runOptions.configFile));
        Map cfgOptions = (Map) reader.read();
        logger.info("cfgOptions: {}", cfgOptions);
        final String bindAddr = (String) cfgOptions.get("bind_addr");
        final int bindPort = Integer.parseInt(cfgOptions.get("bind_port").toString());
        final String token = (String) cfgOptions.get("token");
        new TunnelServer(
                bindAddr,
                bindPort,
                new OpenTunnelRequestInterceptor(token)
        ).start();
    }

    private void runAppAtArgs(@NotNull final RunOptions runOptions) throws Exception {
        new TunnelServer(
                runOptions.bindAddr.length() == 0 ? null : runOptions.bindAddr,
                runOptions.bindPort,
                new OpenTunnelRequestInterceptor(runOptions.token)
        ).start();
    }

    private static class OpenTunnelRequestInterceptor implements Interceptor<OpenTunnelRequest> {
        @NotNull
        private final String token;

        public OpenTunnelRequestInterceptor(@NotNull String token) {
            this.token = token;
        }

        @NotNull
        @Override
        public OpenTunnelRequest proceed(@NotNull OpenTunnelRequest request) throws TunnelProtocolException {
            String token = request.arguments.get("token");
            if (!this.token.equals(token)) {
                throw new TunnelProtocolException("Token Error");
            }
            if (request.remotePort < 1024) {
                throw new TunnelProtocolException("remotePort Error");
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
    }
}
