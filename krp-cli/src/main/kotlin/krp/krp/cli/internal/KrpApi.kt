@file:Suppress("DuplicatedCode")

package krp.krp.cli.internal

import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.handler.codec.http.*
import krp.common.utils.ManifestUtils
import krp.common.utils.basicAuthorization
import krp.extensions.name
import krp.httpd.Httpd
import krp.krp.Krp
import org.json.JSONArray
import org.json.JSONObject
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*
import kotlin.concurrent.getOrSet

internal class KrpApi(private val krp: Krp) {

    fun start(
        bindIp: String?,
        bindPort: Int,
        authProvider: ((username: String, password: String) -> Boolean)?,
    ) {
        Httpd(
            bossGroup = NioEventLoopGroup(),
            workerGroup = NioEventLoopGroup(),
            name = "KrpApi",
            bindIp = bindIp,
            bindPort = bindPort,
        ) {
            intercept("^/.*".toRegex()) {
                val auth = authProvider ?: return@intercept null
                val account = it.basicAuthorization
                val next = if (account != null) auth(account.first, account.second) else false
                if (next) {
                    null
                } else {
                    val content = HttpResponseStatus.UNAUTHORIZED.toString().toByteArray()
                    DefaultFullHttpResponse(it.protocolVersion(), HttpResponseStatus.UNAUTHORIZED).apply {
                        headers().add(HttpHeaderNames.WWW_AUTHENTICATE, "Basic realm=.")
                        headers().add(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE)
                        headers().add(HttpHeaderNames.ACCEPT_RANGES, HttpHeaderValues.BYTES)
                        headers().add(HttpHeaderNames.DATE, Date().toString())
                        headers().add(HttpHeaderNames.CONTENT_LENGTH, content.size)
                        content().writeBytes(content)
                    }
                }
            }
            route("^/version".toRegex()) {
                Unpooled.copiedBuffer(version.toString(2), Charsets.UTF_8)
                    .newFullHttpResponse(HttpHeaderValues.APPLICATION_JSON)
            }
            route("^/snapshot".toRegex()) {
                Unpooled.copiedBuffer(snapshot.toString(2), Charsets.UTF_8)
                    .newFullHttpResponse(HttpHeaderValues.APPLICATION_JSON)
            }
        }.start()
    }

    private val version
        get() = JSONObject(LinkedHashMap<Any, Any>()).apply {
            put("appName", ManifestUtils.appName)
            put("version", ManifestUtils.version)
            put("buildDate", ManifestUtils.buildDate)
            put("commitHash", ManifestUtils.commitHash)
            put("commitDate", ManifestUtils.commitDate)
        }

    private val snapshot
        get() = JSONArray(krp.getTunnelConnectionList().map {
            JSONObject().apply {
                put("name", it.tunnelRequest.name)
                put("request", it.toString())
                put("extras", it.tunnelRequest.extras)
            }
        })

    private val cachedSdf = ThreadLocal<MutableMap<String, DateFormat>>()

    private fun Date?.format(pattern: String = "yyyy-MM-dd HH:mm:ss"): String? =
        this?.let { getDateFormat(pattern).format(this) }

    private fun getDateFormat(pattern: String) = cachedSdf.getOrSet { hashMapOf() }[pattern] ?: SimpleDateFormat(
        pattern, Locale.getDefault()
    ).also { cachedSdf.get()[pattern] = it }

    private fun ByteBuf.newFullHttpResponse(contentType: CharSequence) = DefaultFullHttpResponse(
        HttpVersion.HTTP_1_1, HttpResponseStatus.OK, this
    ).apply {
        headers().set(HttpHeaderNames.CONTENT_TYPE, contentType)
            .set(HttpHeaderNames.CONTENT_LENGTH, this@newFullHttpResponse.readableBytes())
    }
}
