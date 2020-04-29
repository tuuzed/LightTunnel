package lighttunnel.api.server

import io.netty.buffer.Unpooled
import io.netty.channel.ChannelFutureListener
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler
import io.netty.handler.codec.http.*


class ApiServerChannelHandler(
    private val server: ApiServer
) : SimpleChannelInboundHandler<FullHttpRequest>() {

    override fun channelReadComplete(ctx: ChannelHandlerContext) {
        ctx.writeAndFlush(Unpooled.EMPTY_BUFFER)
    }

    @Throws(Exception::class)
    override fun channelRead0(ctx: ChannelHandlerContext, request: FullHttpRequest) {
        if (HttpUtil.is100ContinueExpected(request)) {
            ctx.write(DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1,
                HttpResponseStatus.CONTINUE)
            )
        }
        val response = server.doRequest(request)
        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE)
    }

}