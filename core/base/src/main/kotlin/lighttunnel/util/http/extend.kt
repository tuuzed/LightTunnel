@file:JvmName("-ExtendKt")

package lighttunnel.util.http

import io.netty.buffer.ByteBuf
import io.netty.buffer.ByteBufUtil
import io.netty.handler.codec.http.HttpRequest
import io.netty.handler.codec.http.HttpResponse
import lighttunnel.util.HttpUtil


val HttpRequest.domainHost: String? get() = HttpUtil.getDomainHost(this)
val HttpRequest.basicAuthorization: Array<String>? get() = HttpUtil.getBasicAuthorization(this)
fun HttpRequest.toBytes(): ByteArray = ByteBufUtil.getBytes(HttpUtil.toByteBuf(this))
fun HttpResponse.toByteBuf(): ByteBuf = HttpUtil.toByteBuf(this)