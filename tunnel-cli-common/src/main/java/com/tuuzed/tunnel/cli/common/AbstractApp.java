package com.tuuzed.tunnel.cli.common;

import org.jetbrains.annotations.NotNull;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.OptionHandlerFilter;
import org.kohsuke.args4j.spi.OptionHandler;

public abstract class AbstractApp<Options> {
    @NotNull
    public abstract Options newRunOptions();

    public abstract void runApp(@NotNull Options runOptions);

    public final void doMain(String[] args) {
        Options runOptions = newRunOptions();
        CmdLineParser parser = new CmdLineParser(runOptions);
        parser.printExample(new OptionHandlerFilter() {
            @Override
            public boolean select(OptionHandler o) {
                return true;
            }
        });
        try {
            if (args == null || args.length == 0) {
                throw new Exception();
            }
            parser.parseArgument(args);
        } catch (Exception e) {
            System.err.println("Usage: ");
            parser.printUsage(System.err);
            System.exit(0);
        }
        runApp(runOptions);
    }


}
