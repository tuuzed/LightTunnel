package lighttunnel.api.client

import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler
import io.netty.handler.codec.http.FullHttpResponse

class ApiClientChannelHandler : SimpleChannelInboundHandler<FullHttpResponse>() {

    override fun channelInactive(ctx: ChannelHandlerContext?) {
        super.channelInactive(ctx)
        ctx?.channel()?.attr(ApiClient.AK_REQUEST_CALLBACK)?.set(null)
    }

    override fun channelRead0(ctx: ChannelHandlerContext?, msg: FullHttpResponse?) {
        ctx ?: return
        msg ?: return
        val callback = ctx.channel().attr(ApiClient.AK_REQUEST_CALLBACK).get()
        callback?.invoke(msg)
    }

}