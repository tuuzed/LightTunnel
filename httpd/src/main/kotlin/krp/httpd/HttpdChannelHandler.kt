package krp.httpd

import io.netty.buffer.Unpooled
import io.netty.channel.ChannelFutureListener
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler
import io.netty.handler.codec.http.*

internal class HttpdChannelHandler(
    private val dispatcher: (request: FullHttpRequest) -> FullHttpResponse
) : SimpleChannelInboundHandler<FullHttpRequest>() {

    override fun channelReadComplete(ctx: ChannelHandlerContext) {
        ctx.writeAndFlush(Unpooled.EMPTY_BUFFER)
        super.channelReadComplete(ctx)
    }

    @Throws(Exception::class)
    override fun channelRead0(ctx: ChannelHandlerContext, request: FullHttpRequest) {
        if (HttpUtil.is100ContinueExpected(request)) {
            ctx.writeAndFlush(
                DefaultFullHttpResponse(
                    HttpVersion.HTTP_1_1,
                    HttpResponseStatus.CONTINUE
                )
            )
        }
        val response = dispatcher(request)
        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE)
    }

}
