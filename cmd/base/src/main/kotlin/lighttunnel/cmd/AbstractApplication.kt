package lighttunnel.cmd

import org.apache.commons.cli.CommandLine
import org.apache.commons.cli.DefaultParser
import org.apache.commons.cli.Options
import org.apache.commons.cli.ParseException

abstract class AbstractApplication {

    fun execute(args: Array<String>) {
        val parser = DefaultParser()
        try {
            val commandLine = parser.parse(options, args)
            run(commandLine)
        } catch (e: ParseException) {
            printUsage()
        }
    }

    abstract val options: Options

    @Throws(ParseException::class)
    abstract fun run(commandLine: CommandLine)

    private fun printUsage() {
        val maxSpaceCount = options.options.map { it.opt.length + it.longOpt.length }.max() ?: 0
        println()
        System.out.printf("命令参数：%n")
        options.options.forEach {
            val spaces = " ".repeat(maxSpaceCount - (it.opt.length + it.longOpt.length))
            System.out.printf("  -%s, --%s${spaces}\t%s%n", it.opt, it.longOpt, it.description)
        }
        println()
    }

}