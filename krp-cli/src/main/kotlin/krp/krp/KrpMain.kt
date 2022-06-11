@file:Suppress("DuplicatedCode", "CAST_NEVER_SUCCEEDS")

package krp.krp

import com.github.ajalt.clikt.core.subcommands
import krp.krp.cli.KrpCommand
import krp.krp.cli.KrpHttpCommand
import krp.krp.cli.KrpHttpsCommand
import krp.krp.cli.KrpTcpCommand

fun main(args: Array<String>) = KrpCommand().subcommands(
    KrpTcpCommand(),
    KrpHttpCommand(),
    KrpHttpsCommand(),
).main(args)

