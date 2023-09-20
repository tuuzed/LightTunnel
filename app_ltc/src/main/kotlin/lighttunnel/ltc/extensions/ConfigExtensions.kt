package lighttunnel.ltc.extensions

import lighttunnel.app.base.*
import lighttunnel.common.entity.TunnelRequest
import lighttunnel.common.extensions.getOrNull
import lighttunnel.common.utils.IpUtils
import lighttunnel.common.utils.ManifestUtils
import org.json.JSONObject
import java.util.*


fun JSONObject.asTunnelRequest(authTokenOrNull: String?): TunnelRequest? {
    val type = getOrNull<String>("type") ?: "tcp"
    return when (type.uppercase()) {
        "TCP" -> getTcpTunnelRequest(this, authTokenOrNull)
        "HTTP" -> getHttpOrHttpsTunnelRequest(this, false, authTokenOrNull)
        "HTTPS" -> getHttpOrHttpsTunnelRequest(this, true, authTokenOrNull)
        else -> null
    }
}

private fun getTcpTunnelRequest(tunnel: JSONObject, authTokenOrNull: String?): TunnelRequest {
    return TunnelRequest.forTcp(
        localIp = tunnel.getOrNull<String>("local_ip") ?: IpUtils.localIpV4 ?: "127.0.0.1",
        localPort = tunnel.getOrNull<Int>("local_port") ?: 80,
        remotePort = tunnel.getOrNull<Int>("remote_port") ?: 0
    ) {
        name = tunnel.getOrNull<String>("name") ?: "tcp_${UUID.randomUUID().toString().replace("-", "")}"
        os = "${System.getProperty("os.name")}-${System.getProperty("os.arch")}-${System.getProperty("os.version")}"
        version = ManifestUtils.version
        authToken = authTokenOrNull
    }
}

private fun getHttpOrHttpsTunnelRequest(tunnel: JSONObject, https: Boolean, authTokenOrNull: String?): TunnelRequest? {
    val proxyRewriteHeaders = tunnel.keySet()
        .filter { it.startsWith("pxy_header_rewrite_") }
        .mapNotNull { k ->
            val v = tunnel.getOrNull<String>(k) ?: return@mapNotNull null
            k.substring("pxy_header_rewrite_".length) to v
        }
        .toMap()
    val proxyAddHeaders = tunnel.keySet()
        .filter { it.startsWith("pxy_header_add_") }
        .mapNotNull { k ->
            val v = tunnel.getOrNull<String>(k) ?: return@mapNotNull null
            k.substring("pxy_header_add_".length) to v
        }
        .toMap()
    return TunnelRequest.forHttp(
        https = https,
        localIp = tunnel.getOrNull<String>("local_ip") ?: IpUtils.localIpV4 ?: "127.0.0.1",
        localPort = tunnel.getOrNull<Int>("local_port") ?: 80,
        vhost = tunnel.getOrNull<String>("vhost") ?: return null
    ) {
        name = tunnel.getOrNull<String>("name") ?: "${if (https) "https" else "http"}_${UUID.randomUUID().toString().replace("-", "")}"
        os = "${System.getProperty("os.name")}-${System.getProperty("os.arch")}-${System.getProperty("os.version")}"
        version = ManifestUtils.version
        authToken = authTokenOrNull
        pxyRewriteHeaders = proxyRewriteHeaders
        pxyAddHeaders = proxyAddHeaders
        enableBasicAuth = tunnel["auth_username"] != null && tunnel["auth_password"] != null
        if (enableBasicAuth) {
            basicAuthRealm = tunnel.getOrNull("auth_realm") ?: "."
            basicAuthUsername = tunnel.getOrNull("auth_username")
            basicAuthPassword = tunnel.getOrNull("auth_password")
        }
    }
}
