package lighttunnel.app.base

import lighttunnel.common.entity.TunnelRequest
import lighttunnel.common.extensions.optOrNull
import org.json.JSONObject

private const val NAME = "ext.NAME"
private const val VERSION = "ext.VERSION"
private const val OS = "ext.OS"
private const val AUTH_TOKEN = "ext.AUTH_TOKEN"
private const val ENABLE_BASIC_AUTH = "ext.ENABLE_BASIC_AUTH"
private const val BASIC_AUTH_REALM = "ext.BASIC_AUTH_REALM"
private const val BASIC_AUTH_USERNAME = "ext.BASIC_AUTH_USERNAME"
private const val BASIC_AUTH_PASSWORD = "ext.BASIC_AUTH_PASSWORD"
private const val PXY_REWRITE_HEADERS = "ext.PXY_REWRITE_HEADERS"
private const val PXY_ADD_HEADERS = "ext.PXY_ADD_HEADERS"

var TunnelRequest.name: String?
    get() = extras.optOrNull(NAME)
    set(value) {
        if (value == null) extras.remove(NAME) else extras.put(NAME, value)
    }

var TunnelRequest.version: String?
    get() = extras.optOrNull(VERSION)
    set(value) {
        if (value == null) extras.remove(VERSION) else extras.put(VERSION, value)
    }
var TunnelRequest.os: String?
    get() = extras.optOrNull(OS)
    set(value) {
        if (value == null) extras.remove(OS) else extras.put(OS, value)
    }

var TunnelRequest.authToken: String?
    get() = extras.optOrNull(AUTH_TOKEN)
    set(value) {
        if (value == null) extras.remove(AUTH_TOKEN) else extras.put(AUTH_TOKEN, value)
    }

var TunnelRequest.enableBasicAuth: Boolean
    get() = extras.optOrNull<Boolean>(ENABLE_BASIC_AUTH) == true
    set(value) {
        if (!value) extras.remove(ENABLE_BASIC_AUTH) else extras.put(ENABLE_BASIC_AUTH, value)
    }

var TunnelRequest.basicAuthRealm: String?
    get() = extras.optOrNull(BASIC_AUTH_REALM)
    set(value) {
        if (value == null) extras.remove(BASIC_AUTH_REALM) else extras.put(BASIC_AUTH_REALM, value)
    }

var TunnelRequest.basicAuthUsername: String?
    get() = extras.optOrNull(BASIC_AUTH_USERNAME)
    set(value) {
        if (value == null) extras.remove(BASIC_AUTH_USERNAME) else extras.put(BASIC_AUTH_USERNAME, value)
    }

var TunnelRequest.basicAuthPassword: String?
    get() = extras.optOrNull(BASIC_AUTH_PASSWORD)
    set(value) {
        if (value == null) extras.remove(BASIC_AUTH_PASSWORD) else extras.put(BASIC_AUTH_PASSWORD, value)
    }

var TunnelRequest.pxyRewriteHeaders: Map<String, String>
    get() = extras.optOrNull<JSONObject>(PXY_REWRITE_HEADERS).toStringMap()
    set(value) {
        if (value.isEmpty()) extras.remove(PXY_REWRITE_HEADERS) else extras.put(PXY_REWRITE_HEADERS, value)
    }

var TunnelRequest.pxyAddHeaders: Map<String, String>
    get() = extras.optOrNull<JSONObject>(PXY_ADD_HEADERS).toStringMap()
    set(value) {
        if (value.isEmpty()) extras.remove(PXY_ADD_HEADERS) else extras.put(PXY_ADD_HEADERS, value)
    }

private fun JSONObject?.toStringMap(): Map<String, String> {
    this ?: return emptyMap()
    return keySet().filterNotNull().mapNotNull { k ->
        val v = optOrNull<String>(k) ?: return@mapNotNull null
        k to v
    }.toMap()
}

