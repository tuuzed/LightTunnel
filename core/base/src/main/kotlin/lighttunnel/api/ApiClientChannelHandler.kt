package lighttunnel.api

import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler
import io.netty.handler.codec.http.FullHttpResponse

class ApiClientChannelHandler : SimpleChannelInboundHandler<FullHttpResponse>() {

    override fun channelInactive(ctx: ChannelHandlerContext?) {
        super.channelInactive(ctx)
        ctx?.channel()?.attr(ApiClient.RESPONSE_CALLBACK)?.set(null)
    }

    override fun channelRead0(ctx: ChannelHandlerContext?, msg: FullHttpResponse?) {
        ctx ?: return
        msg ?: return
        val callback = ctx.channel().attr<ApiClient.ResponseCallback>(ApiClient.RESPONSE_CALLBACK).get()
        callback?.onResponse(msg)
    }

}