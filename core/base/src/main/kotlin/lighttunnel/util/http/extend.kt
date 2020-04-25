@file:JvmName("-ExtendKt")

package lighttunnel.util.http

import io.netty.handler.codec.http.HttpRequest
import io.netty.handler.codec.http.HttpResponse
import lighttunnel.util.HttpUtil


val HttpRequest.domainHost: String? get() = HttpUtil.getDomainHost(this)
val HttpRequest.basicAuthorization: Array<String>? get() = HttpUtil.getBasicAuthorization(this)
fun HttpRequest.toBytes() = HttpUtil.toBytes(this)
fun HttpResponse.toByteBuf() = HttpUtil.toByteBuf(this)