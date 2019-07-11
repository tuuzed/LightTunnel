package com.tuuzed.tunnel.cli;

import com.tuuzed.tunnel.common.logging.Logger;
import com.tuuzed.tunnel.common.logging.LoggerFactory;
import com.tuuzed.tunnel.server.TunnelServer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.kohsuke.args4j.Option;

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
            new TunnelServer(
                    runOptions.bindAddr,
                    runOptions.bindPort
            ).start();
        } catch (Exception e) {
            logger.error("runApp Error: {}", e.getMessage(), e);
        }
    }

    static class RunOptions {
        @Nullable
        @Option(name = "-h", aliases = {"-bindAddr"}, help = true, metaVar = "<string>", usage = "绑定地址")
        public String bindAddr = null;

        @Option(name = "-p", aliases = {"-bindPort"}, help = true, metaVar = "<int>", usage = "绑定端口")
        public int bindPort = 5000;
    }

}
