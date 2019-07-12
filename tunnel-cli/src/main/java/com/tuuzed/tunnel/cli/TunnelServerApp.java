package com.tuuzed.tunnel.cli;

import com.esotericsoftware.yamlbeans.YamlReader;
import com.tuuzed.tunnel.common.logging.Logger;
import com.tuuzed.tunnel.common.logging.LoggerFactory;
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
        new TunnelServer(
                bindAddr,
                bindPort
        ).start();
    }

    private void runAppAtArgs(@NotNull RunOptions runOptions) throws Exception {
        new TunnelServer(
                runOptions.bindAddr.length() == 0 ? null : runOptions.bindAddr,
                runOptions.bindPort
        ).start();
    }

    static class RunOptions {
        @Option(name = "-c", aliases = {"--configFile"}, help = true, metaVar = "<string>", usage = "配置文件，当设置了配置文件时优先使用配置文件配置项")
        public String configFile = "";

        @Option(name = "-h", aliases = {"-bindAddr"}, help = true, metaVar = "<string>", usage = "绑定地址")
        public String bindAddr = "";

        @Option(name = "-p", aliases = {"-bindPort"}, help = true, metaVar = "<int>", usage = "绑定端口")
        public int bindPort = 5000;
    }
}
