package com.tuuzed.tunnel.cli;

import com.esotericsoftware.yamlbeans.YamlReader;
import com.tuuzed.tunnel.common.logging.Logger;
import com.tuuzed.tunnel.common.logging.LoggerFactory;
import com.tuuzed.tunnel.common.protocol.OpenTunnelRequest;
import com.tuuzed.tunnel.server.Interceptor;
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
        Map options = (Map) reader.read();
        logger.info("options: {}", options);
        final String bindAddr = (String) options.get("bind_addr");
        final int bindPort = Integer.parseInt(options.get("bind_port").toString());
        final String token = (String) options.get("token");
        new TunnelServer(
                bindAddr,
                bindPort,
                new Interceptor<OpenTunnelRequest>() {
                    @Override
                    public void proceed(@NotNull OpenTunnelRequest request) throws Exception {
                        if (!"tk123456".equals(token)) {
                            throw new Exception("Token Error");
                        }
                        if (request.remotePort < 10000) {
                            throw new Exception("remotePort Error");
                        }
                    }
                }
        ).start();
    }

    private void runAppAtArgs(@NotNull final RunOptions runOptions) throws Exception {
        new TunnelServer(
                runOptions.bindAddr.length() == 0 ? null : runOptions.bindAddr,
                runOptions.bindPort,
                new Interceptor<OpenTunnelRequest>() {
                    @Override
                    public void proceed(@NotNull OpenTunnelRequest request) throws Exception {
                        if (!"tk123456".equals(runOptions.token)) {
                            throw new Exception("Token Error");
                        }
                        if (request.remotePort < 10000) {
                            throw new Exception("remotePort Error");
                        }
                    }
                }
        ).start();
    }

    static class RunOptions {
        @Option(name = "-c", aliases = {"--configFile"}, help = true, metaVar = "<string>", usage = "配置文件，当设置了配置文件时优先使用配置文件配置项")
        public String configFile = "";

        @Option(name = "-a", aliases = {"--bindAddr"}, help = true, metaVar = "<string>", usage = "绑定地址")
        public String bindAddr = "";

        @Option(name = "-p", aliases = {"--bindPort"}, help = true, metaVar = "<int>", usage = "绑定端口")
        public int bindPort = 5000;

        @Option(name = "-t", aliases = {"--token"}, help = true, metaVar = "<int>", usage = "令牌")
        public String token = "";
    }
}
