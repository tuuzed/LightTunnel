package lighttunnel.ltc.extensions

import lighttunnel.app.base.*
import lighttunnel.common.entity.TunnelRequest
import lighttunnel.common.extensions.optOrNull
import lighttunnel.common.utils.IpUtils
import lighttunnel.common.utils.ManifestUtils
import org.json.JSONObject
import java.util.*


fun JSONObject.asTunnelRequest(authTokenOrNull: String?): TunnelRequest? {
    val type = optOrNull<String>("type") ?: "tcp"
    return when (type.uppercase()) {
        "TCP" -> getTcpTunnelRequest(this, authTokenOrNull)
        "HTTP" -> getHttpOrHttpsTunnelRequest(this, false, authTokenOrNull)
        "HTTPS" -> getHttpOrHttpsTunnelRequest(this, true, authTokenOrNull)
        else -> null
    }
}

private fun getTcpTunnelRequest(tunnel: JSONObject, authTokenOrNull: String?): TunnelRequest {
    return TunnelRequest.forTcp(
        localIp = tunnel.optOrNull<String>("local_ip") ?: IpUtils.localIpV4 ?: "127.0.0.1",
        localPort = tunnel.optOrNull<Int>("local_port") ?: 80,
        remotePort = tunnel.optOrNull<Int>("remote_port") ?: 0
    ) {
        name = tunnel.optOrNull<String>("name") ?: "tcp_${UUID.randomUUID().toString().replace("-", "")}"
        os = "${System.getProperty("os.name")}-${System.getProperty("os.arch")}-${System.getProperty("os.version")}"
        version = ManifestUtils.version
        authToken = authTokenOrNull
    }
}

private fun getHttpOrHttpsTunnelRequest(tunnel: JSONObject, https: Boolean, authTokenOrNull: String?): TunnelRequest? {
    val proxyRewriteHeaders = tunnel.keySet()
        .filter { it.startsWith("pxy_header_rewrite_") }
        .mapNotNull { k ->
            val v = tunnel.optOrNull<String>(k) ?: return@mapNotNull null
            k.substring("pxy_header_rewrite_".length) to v
        }
        .toMap()
    val proxyAddHeaders = tunnel.keySet()
        .filter { it.startsWith("pxy_header_add_") }
        .mapNotNull { k ->
            val v = tunnel.optOrNull<String>(k) ?: return@mapNotNull null
            k.substring("pxy_header_add_".length) to v
        }
        .toMap()
    return TunnelRequest.forHttp(
        https = https,
        localIp = tunnel.optOrNull<String>("local_ip") ?: IpUtils.localIpV4 ?: "127.0.0.1",
        localPort = tunnel.optOrNull<Int>("local_port") ?: 80,
        vhost = tunnel.optOrNull<String>("vhost") ?: return null
    ) {
        name = tunnel.optOrNull<String>("name") ?: "${if (https) "https" else "http"}_${UUID.randomUUID().toString().replace("-", "")}"
        os = "${System.getProperty("os.name")}-${System.getProperty("os.arch")}-${System.getProperty("os.version")}"
        version = ManifestUtils.version
        authToken = authTokenOrNull
        pxyRewriteHeaders = proxyRewriteHeaders
        pxyAddHeaders = proxyAddHeaders
        enableBasicAuth = tunnel["auth_username"] != null && tunnel["auth_password"] != null
        if (enableBasicAuth) {
            basicAuthRealm = tunnel.optOrNull("auth_realm") ?: "."
            basicAuthUsername = tunnel.optOrNull("auth_username")
            basicAuthPassword = tunnel.optOrNull("auth_password")
        }
    }
}
