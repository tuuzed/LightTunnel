package lighttunnel.ext

import lighttunnel.TunnelRequest
import lighttunnel.internal.base.util.getOrNull
import org.json.JSONObject

private const val NAME = "ext.NAME"
private const val VERSION = "ext.VERSION"
private const val AUTH_TOKEN = "ext.AUTH_TOKEN"
private const val ENABLE_BASIC_AUTH = "ext.ENABLE_BASIC_AUTH"
private const val BASIC_AUTH_REALM = "ext.BASIC_AUTH_REALM"
private const val BASIC_AUTH_USERNAME = "ext.BASIC_AUTH_USERNAME"
private const val BASIC_AUTH_PASSWORD = "ext.BASIC_AUTH_PASSWORD"
private const val PXY_SET_HEADERS = "ext.PXY_SET_HEADERS"
private const val PXY_ADD_HEADERS = "ext.PXY_ADD_HEADERS"

var TunnelRequest.name: String?
    get() = extras.getOrNull(NAME)
    set(value) = run { if (value == null) extras.remove(NAME) else extras.put(NAME, value) }

var TunnelRequest.version: String?
    get() = extras.getOrNull(VERSION)
    set(value) = run { if (value == null) extras.remove(VERSION) else extras.put(VERSION, value) }

var TunnelRequest.authToken: String?
    get() = extras.getOrNull(AUTH_TOKEN)
    set(value) = run { if (value == null) extras.remove(AUTH_TOKEN) else extras.put(AUTH_TOKEN, value) }

var TunnelRequest.enableBasicAuth: Boolean
    get() = extras.getOrNull<Boolean>(ENABLE_BASIC_AUTH) == true
    set(value) = run { if (!value) extras.remove(ENABLE_BASIC_AUTH) else extras.put(ENABLE_BASIC_AUTH, value) }

var TunnelRequest.basicAuthRealm: String?
    get() = extras.getOrNull(BASIC_AUTH_REALM)
    set(value) = run { if (value == null) extras.remove(BASIC_AUTH_REALM) else extras.put(BASIC_AUTH_REALM, value) }

var TunnelRequest.basicAuthUsername: String?
    get() = extras.getOrNull(BASIC_AUTH_USERNAME)
    set(value) = run { if (value == null) extras.remove(BASIC_AUTH_USERNAME) else extras.put(BASIC_AUTH_USERNAME, value) }


var TunnelRequest.basicAuthPassword: String?
    get() = extras.getOrNull(BASIC_AUTH_PASSWORD)
    set(value) = run { if (value == null) extras.remove(BASIC_AUTH_PASSWORD) else extras.put(BASIC_AUTH_PASSWORD, value) }


var TunnelRequest.pxySetHeaders: Map<String, String>
    get() = extras.getOrNull<JSONObject>(PXY_SET_HEADERS).toStringMap()
    set(value) = run { if (value.isEmpty()) extras.remove(PXY_SET_HEADERS) else extras.put(PXY_SET_HEADERS, value) }

var TunnelRequest.pxyAddHeaders: Map<String, String>
    get() = extras.getOrNull<JSONObject>(PXY_ADD_HEADERS).toStringMap()
    set(value) = run { if (value.isEmpty()) extras.remove(PXY_ADD_HEADERS) else extras.put(PXY_ADD_HEADERS, value) }

private fun JSONObject?.toStringMap(): Map<String, String> {
    this ?: return emptyMap()
    val map = mutableMapOf<String, String>()
    keys().forEach { key -> getOrNull<String>(key)?.also { value -> map[key] = value } }
    return map
}

