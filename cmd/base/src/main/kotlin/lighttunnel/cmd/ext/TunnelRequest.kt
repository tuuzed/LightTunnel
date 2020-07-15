package lighttunnel.cmd.ext

import lighttunnel.base.util.toStringMap
import lighttunnel.openapi.TunnelRequest
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
    get() = getExtra(NAME)
    set(value) = if (value == null) removeExtra(NAME) else setExtra(NAME, value)

var TunnelRequest.version: String?
    get() = getExtra(VERSION)
    set(value) = if (value == null) removeExtra(VERSION) else setExtra(VERSION, value)

var TunnelRequest.authToken: String?
    get() = getExtra(AUTH_TOKEN)
    set(value) = if (value == null) removeExtra(AUTH_TOKEN) else setExtra(AUTH_TOKEN, value)

var TunnelRequest.enableBasicAuth: Boolean
    get() = getExtra<Boolean?>(ENABLE_BASIC_AUTH) == true
    set(value) = if (!value) removeExtra(ENABLE_BASIC_AUTH) else setExtra(ENABLE_BASIC_AUTH, value)

var TunnelRequest.basicAuthRealm: String?
    get() = getExtra(BASIC_AUTH_REALM)
    set(value) = if (value == null) removeExtra(BASIC_AUTH_REALM) else setExtra(BASIC_AUTH_REALM, value)

var TunnelRequest.basicAuthUsername: String?
    get() = getExtra(BASIC_AUTH_USERNAME)
    set(value) = if (value == null) removeExtra(BASIC_AUTH_USERNAME) else setExtra(BASIC_AUTH_USERNAME, value)


var TunnelRequest.basicAuthPassword: String?
    get() = getExtra(BASIC_AUTH_PASSWORD)
    set(value) = if (value == null) removeExtra(BASIC_AUTH_PASSWORD) else setExtra(BASIC_AUTH_PASSWORD, value)


var TunnelRequest.pxySetHeaders: Map<String, String>
    get() = getExtra<JSONObject>(PXY_SET_HEADERS).toStringMap()
    set(value) = if (value.isEmpty()) removeExtra(PXY_SET_HEADERS) else setExtra(PXY_SET_HEADERS, JSONObject(value))

var TunnelRequest.pxyAddHeaders: Map<String, String>
    get() = getExtra<JSONObject>(PXY_ADD_HEADERS).toStringMap()
    set(value) = if (value.isEmpty()) removeExtra(PXY_ADD_HEADERS) else setExtra(PXY_ADD_HEADERS, JSONObject(value))


