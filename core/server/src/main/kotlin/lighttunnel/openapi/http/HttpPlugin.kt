package lighttunnel.openapi.http

import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.http.HttpContent
import io.netty.handler.codec.http.HttpRequest
import io.netty.handler.codec.http.HttpResponse
import lighttunnel.base.util.byteBuf
import java.io.IOException

interface HttpPlugin {

    @Throws(IOException::class)
    fun doHttpRequest(chain: HttpChain, httpRequest: HttpRequest): Boolean

    @Throws(IOException::class)
    fun doHttpContent(chain: HttpChain, httpContent: HttpContent)

    fun ChannelHandlerContext.writeHttpResponse(response: HttpResponse, flush: Boolean = false) {
        if (flush) {
            this.write(response.byteBuf)
        } else {
            this.writeAndFlush(response.byteBuf)
        }
    }

    fun ChannelHandlerContext.writeHttpContent(response: HttpContent, flush: Boolean = false) {
        if (flush) {
            this.write(response.byteBuf)
        } else {
            this.writeAndFlush(response.byteBuf)
        }
    }

}