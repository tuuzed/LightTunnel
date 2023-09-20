package lighttunnel.ltc

import com.github.ajalt.clikt.core.subcommands
import lighttunnel.ltc.commond.HttpCommand
import lighttunnel.ltc.commond.HttpsCommand
import lighttunnel.ltc.commond.RootCommand
import lighttunnel.ltc.commond.TcpCommand

fun main(args: Array<String>) = RootCommand().subcommands(
    TcpCommand(),
    HttpCommand(),
    HttpsCommand(),
).main(args)

