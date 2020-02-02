package lighttunnel.dashboard.client

import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler
import io.netty.handler.codec.http.FullHttpResponse

class DashboardClientChannelHandler : SimpleChannelInboundHandler<FullHttpResponse>() {

    override fun channelInactive(ctx: ChannelHandlerContext?) {
        super.channelInactive(ctx)
        ctx?.channel()?.attr(DashboardClient.REQUEST_CALLBACK)?.set(null)
    }

    override fun channelRead0(ctx: ChannelHandlerContext?, msg: FullHttpResponse?) {
        ctx ?: return
        msg ?: return
        val callback = ctx.channel().attr<RequestCallback>(DashboardClient.REQUEST_CALLBACK).get()
        callback?.invoke(msg)
    }

}