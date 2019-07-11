package com.tuuzed.tunnel.cli;

import com.tuuzed.tunnel.client.TunnelClient;
import com.tuuzed.tunnel.common.logging.Logger;
import com.tuuzed.tunnel.common.logging.LoggerFactory;
import org.jetbrains.annotations.NotNull;
import org.kohsuke.args4j.Option;

public class TunnelClientApp extends AbstractApp<TunnelClientApp.RunOptions> {
    private static final Logger logger = LoggerFactory.getLogger(TunnelClientApp.class);

    @NotNull
    @Override
    RunOptions newRunOptions() {
        return new RunOptions();
    }

    @Override
    void runApp(@NotNull RunOptions runOptions) {
        try {
            new TunnelClient(
                    runOptions.serverAddr,
                    runOptions.serverPort,
                    runOptions.localAddr,
                    runOptions.localPort,
                    runOptions.remotePort
            ).start().sync();
        } catch (Exception e) {
            logger.error("runApp Error: {}", e.getMessage(), e);
        }

    }


    static class RunOptions {
        @Option(name = "-h", aliases = {"--serverAddr"}, help = true, metaVar = "<string>", usage = "服务器地址")
        public String serverAddr = "127.0.0.1";

        @Option(name = "-p", aliases = {"--serverPort"}, help = true, metaVar = "<int>", usage = "服务器端口")
        public int serverPort = 5000;

        @Option(name = "-lh", aliases = {"--localAddr"}, help = true, metaVar = "<string>", usage = "内网地址")
        public String localAddr = "127.0.0.1";

        @Option(name = "-lp", aliases = {"--localPort"}, help = true, metaVar = "<int>", usage = "内网端口")
        public int localPort = 80;

        @Option(name = "-rp", aliases = {"--remotePort"}, help = true, metaVar = "<int>", usage = "映射外网端口")
        public int remotePort = 10080;

    }

}
