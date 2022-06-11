@file:Suppress("CAST_NEVER_SUCCEEDS", "DuplicatedCode")

package krp.krp.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.requireObject
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.int
import krp.common.entity.TunnelRequest
import krp.common.utils.ManifestUtils
import krp.extensions.authToken
import krp.extensions.name
import krp.extensions.os
import krp.extensions.version
import krp.krp.Krp
import krp.krp.cli.internal.DefaultKrpListener
import java.util.*
import kotlin.experimental.or

internal class KrpTcpCommand : CliktCommand(name = "tcp") {

    private val config by requireObject<Map<String, String>>()
    private val workerThreads get() = config["WORKER_THREADS"] as Int?
    private val serverAddr get() = config["SERVER_ADDR"] as Pair<String, Int>
    private val localAddr get() = config["LOCAL_ADDR"] as Pair<String, Int>
    private val authToken get() = config["AUTH_TOKEN"]
    private val useEncryption get() = config["USE_ENCRYPTION"] as Boolean

    private val remotePort: Int? by option("-R", "--remote-port", help = "Remote port, default [random]")
        .int()

    override fun run() {
        val remotePort = remotePort ?: 0
        val krp = Krp(
            workerThreads = workerThreads ?: -1,
            retryConnectPolicy = Krp.RETRY_CONNECT_POLICY_LOSE or Krp.RETRY_CONNECT_POLICY_ERROR,
            krpListener = DefaultKrpListener(),
        )
        krp.connect(
            serverIp = serverAddr.first,
            serverPort = serverAddr.second,
            tunnelRequest = TunnelRequest.forTcp(
                localIp = localAddr.first,
                localPort = localAddr.second,
                remotePort = remotePort,
            ) {
                name = "TCP-${UUID.randomUUID()}"
                os = "${System.getProperty("os.name")}-${System.getProperty("os.arch")}-${System.getProperty("os.version")}"
                version = ManifestUtils.version
                if (!this@KrpTcpCommand.authToken.isNullOrEmpty()) {
                    this.authToken = this@KrpTcpCommand.authToken
                }
            },
            useEncryption = useEncryption,
        )
        Thread.currentThread().join()
    }
}
