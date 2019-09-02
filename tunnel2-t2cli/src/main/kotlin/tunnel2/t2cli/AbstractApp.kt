package tunnel2.t2cli

import kotlin.system.exitProcess

abstract class AbstractApp<Options> {
    abstract fun newRunOptions(): Options

    @Throws(Exception::class)
    abstract fun runApp(runOptions: Options)

    fun doMain(args: Array<String>?) {
        val runOptions = newRunOptions()
        try {
            if (args.isNullOrEmpty()) {
                printHelp(runOptions)
            } else {
                CmdLineParser.parse(runOptions, args)
                runApp(runOptions)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            printHelp(runOptions)
            exitProcess(0)
        }

    }

    private fun printHelp(runOptions: Options) {
        System.err.printf("%nManifest: %n")
        System.err.printf("    VersionName    :  %s%n", TunnelManifest.versionName)
        System.err.printf("    VersionCode    :  %s%n", TunnelManifest.versionCode)
        System.err.printf("    LastCommitSHA  :  %s%n", TunnelManifest.lastCommitSHA)
        System.err.printf("    LastCommitDate :  %s%n", TunnelManifest.lastCommitDate)
        System.err.printf("    BuildDate      :  %s%n", TunnelManifest.buildDate)
        System.err.printf("%n%nUsage: %n")
        try {
            CmdLineParser.printHelp(runOptions, System.err, "    ")
        } catch (e: Exception) {
            e.printStackTrace()
        }
        System.err.printf("%n%n")

    }

}
