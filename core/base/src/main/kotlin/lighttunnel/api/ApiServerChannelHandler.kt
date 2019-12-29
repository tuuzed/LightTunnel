package lighttunnel.api

import io.netty.channel.ChannelFutureListener
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler
import io.netty.handler.codec.http.*


class ApiServerChannelHandler(
    private val requestDispatcher: ApiServer.RequestDispatcher
) : SimpleChannelInboundHandler<FullHttpRequest>() {

    override fun channelReadComplete(ctx: ChannelHandlerContext) {
        ctx.flush()
    }

    @Throws(Exception::class)
    override fun channelRead0(ctx: ChannelHandlerContext, request: FullHttpRequest) {
        if (HttpUtil.is100ContinueExpected(request)) {
            ctx.write(DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1,
                HttpResponseStatus.CONTINUE)
            )
        }
        val response = requestDispatcher.doRequest(request)
        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE)
    }
}