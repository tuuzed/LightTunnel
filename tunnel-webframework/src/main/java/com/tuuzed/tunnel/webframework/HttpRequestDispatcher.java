package com.tuuzed.tunnel.webframework;

import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class HttpRequestDispatcher extends SimpleChannelInboundHandler<HttpObject> {

    private static final Logger logger = LoggerFactory.getLogger(HttpRequestDispatcher.class);

    @NotNull
    private final HttpRouter router;

    public HttpRequestDispatcher(@NotNull HttpRouter router) {
        this.router = router;
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        super.exceptionCaught(ctx, cause);
        ctx.channel().close();
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, HttpObject msg) throws Exception {
        FullHttpRequest request = (FullHttpRequest) msg;
        try {
            routing(ctx, request);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private void routing(ChannelHandlerContext ctx, @NotNull FullHttpRequest request) throws Exception {
        HttpResponse httpResponse = router.handle(request);
        if (httpResponse == null) {
            ctx.channel()
                .writeAndFlush(HttpResponses.raw(HttpResponseStatus.NOT_ACCEPTABLE))
                .addListener(ChannelFutureListener.CLOSE);
        } else {
            ctx.channel().writeAndFlush(httpResponse);
        }
    }

}
