@file:Suppress("DuplicatedCode", "CAST_NEVER_SUCCEEDS")

package krp.krp.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.requireObject
import com.github.ajalt.clikt.parameters.options.option
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

internal class KrpHttpsCommand : CliktCommand(name = "https") {

    private val config by requireObject<Map<String, String>>()
    private val workerThreads get() = config["WORKER_THREADS"] as Int?
    private val serverAddr get() = config["SERVER_ADDR"] as Pair<String, Int>
    private val localAddr get() = config["LOCAL_ADDR"] as Pair<String, Int>
    private val authToken get() = config["AUTH_TOKEN"]
    private val useEncryption get() = config["USE_ENCRYPTION"] as Boolean

    private val vhost: String? by option("-H", "--vhost", help = "Virtual host")

    override fun run() {
        if (vhost == null) {
            echo("Virtual host is required", err = true)
            return
        }

        val krp = Krp(
            workerThreads = workerThreads ?: -1,
            retryConnectPolicy = Krp.RETRY_CONNECT_POLICY_LOSE or Krp.RETRY_CONNECT_POLICY_ERROR,
            krpListener = DefaultKrpListener(),
        )
        krp.connect(
            serverIp = serverAddr.first,
            serverPort = serverAddr.second,
            tunnelRequest = TunnelRequest.forHttp(
                https = true,
                localIp = localAddr.first,
                localPort = localAddr.second,
                vhost = vhost!!,
            ) {
                name = "HTTPS-${UUID.randomUUID()}"
                os = "${System.getProperty("os.name")}-${System.getProperty("os.arch")}-${System.getProperty("os.version")}"
                version = ManifestUtils.version
                if (!this@KrpHttpsCommand.authToken.isNullOrEmpty()) {
                    this.authToken = this@KrpHttpsCommand.authToken
                }
            },
            useEncryption = useEncryption,
        )
        Thread.currentThread().join()
    }
}
