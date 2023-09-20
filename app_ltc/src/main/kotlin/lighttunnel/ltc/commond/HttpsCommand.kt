@file:Suppress("CAST_NEVER_SUCCEEDS")

package lighttunnel.ltc.commond

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.requireObject
import com.github.ajalt.clikt.parameters.options.option
import lighttunnel.app.base.authToken
import lighttunnel.app.base.name
import lighttunnel.app.base.os
import lighttunnel.app.base.version
import lighttunnel.client.Client
import lighttunnel.common.entity.TunnelRequest
import lighttunnel.common.utils.ManifestUtils
import lighttunnel.ltc.internal.DefaultClientListener
import java.util.*
import kotlin.experimental.or

internal class HttpsCommand : CliktCommand(name = "https") {

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

        val client = Client(
            workerThreads = workerThreads ?: -1,
            retryConnectPolicy = Client.RETRY_CONNECT_POLICY_LOSE or Client.RETRY_CONNECT_POLICY_ERROR,
            clientListener = DefaultClientListener(),
        )
        client.connect(
            serverIp = serverAddr.first,
            serverPort = serverAddr.second,
            tunnelRequest = TunnelRequest.forHttp(
                https = true,
                localIp = localAddr.first,
                localPort = localAddr.second,
                vhost = vhost!!,
            ) {
                name = "HTTPS-${UUID.randomUUID()}"
                os =
                    "${System.getProperty("os.name")}-${System.getProperty("os.arch")}-${System.getProperty("os.version")}"
                version = ManifestUtils.version
                if (!this@HttpsCommand.authToken.isNullOrEmpty()) {
                    this.authToken = this@HttpsCommand.authToken
                }
            },
            useEncryption = useEncryption,
        )
        Thread.currentThread().join()
    }
}
