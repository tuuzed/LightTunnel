package lighttunnel.cmd

import org.apache.commons.cli.DefaultParser
import org.apache.commons.cli.Options
import org.junit.Test


class CliTest {


    @Test
    fun test_cli() {
        val parser = DefaultParser()
        val options = Options()
        options.addOption("h", "help", false, "Print this usage information")
        options.addOption("v", "verbose", false, "Print out VERBOSE information")
        options.addOption("f", "file", true, "File to save program output to")
        val commandLine = parser.parse(options, arrayOf("-h"))
        println(commandLine.hasOption('h'))
    }

}