package com.tuuzed.tunnelcli;

import org.jetbrains.annotations.NotNull;

public abstract class AbstractApp<Options> {
    @NotNull
    public abstract Options newRunOptions();

    public abstract void runApp(@NotNull Options runOptions) throws Exception;

    public final void doMain(String[] args) {
        Options runOptions = newRunOptions();
        try {
            if (args == null || args.length == 0) {
                printHelp(runOptions);
            } else {
                CmdLineParser.parse(runOptions, args);
                runApp(runOptions);
            }
        } catch (Exception e) {
            e.printStackTrace();
            printHelp(runOptions);
            System.exit(0);
        }
    }

    private void printHelp(@NotNull Options runOptions) {
        System.err.printf("%nManifest: %n");
        System.err.printf("    VersionName    :  %s%n", TunnelManifest.versionName());
        System.err.printf("    VersionCode    :  %s%n", TunnelManifest.versionCode());
        System.err.printf("    LastCommitSHA  :  %s%n", TunnelManifest.lastCommitSHA());
        System.err.printf("    LastCommitDate :  %s%n", TunnelManifest.lastCommitDate());
        System.err.printf("    BuildDate      :  %s%n", TunnelManifest.buildDate());
        System.err.printf("%n%nUsage: %n");
        try {
            CmdLineParser.printHelp(runOptions, System.err, "    ");
        } catch (Exception e1) {
            e1.printStackTrace();
        }
        System.err.printf("%n%n");

    }

}
