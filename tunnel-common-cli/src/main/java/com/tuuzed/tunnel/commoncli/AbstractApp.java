package com.tuuzed.tunnel.commoncli;

import org.jetbrains.annotations.NotNull;

public abstract class AbstractApp<Options> {
    @NotNull
    public abstract Options newRunOptions();

    public abstract void runApp(@NotNull Options runOptions) throws Exception;

    public final void doMain(String[] args) {
        Options runOptions = newRunOptions();
        try {
            if (args == null || args.length == 0) {
                System.err.println("Usage: ");
                CmdLineParser.printHelp(runOptions, System.err);
            } else {
                CmdLineParser.parse(runOptions, args);
                runApp(runOptions);
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("\n\n\n\nUsage: ");
            try {
                CmdLineParser.printHelp(runOptions, System.err);
            } catch (Exception e1) {
                e1.printStackTrace();
            }
            System.exit(0);
        }
    }

}
