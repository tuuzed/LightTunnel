package lighttunnel.ltc

import com.github.ajalt.clikt.core.subcommands
import lighttunnel.ltc.cli.HttpCommand
import lighttunnel.ltc.cli.HttpsCommand
import lighttunnel.ltc.cli.RootCommand
import lighttunnel.ltc.cli.TcpCommand

fun main(args: Array<String>) = RootCommand().subcommands(
    TcpCommand(),
    HttpCommand(),
    HttpsCommand(),
).main(args)

