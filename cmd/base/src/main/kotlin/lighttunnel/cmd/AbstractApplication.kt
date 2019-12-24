package lighttunnel.cmd

import org.apache.commons.cli.CommandLine
import org.apache.commons.cli.DefaultParser
import org.apache.commons.cli.Options

abstract class AbstractApplication {

    fun execute(args: Array<String>) {
        val parser = DefaultParser()
        val commandLine = parser.parse(options, args)
        main(commandLine)
    }

    abstract val options: Options


    fun printUsage() {
        System.out.printf("Usage:%n")
        options.options.forEach {
            System.out.printf("-%s, --%s\t%s%n", it.opt, it.longOpt, it.description)
        }
    }

    abstract fun main(commandLine: CommandLine)

}