package com.tuuzed.tunnel.web.framework;

import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;


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
            FullHttpResponse response = HttpResponses.basic(HttpResponseStatus.INTERNAL_SERVER_ERROR);
            response.content().writeBytes("\r\n".getBytes(StandardCharsets.UTF_8));
            response.content().writeBytes(e.toString().getBytes(StandardCharsets.UTF_8));
            ctx.channel().writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
        }
    }


    private void routing(ChannelHandlerContext ctx, @NotNull FullHttpRequest request) throws Exception {
        HttpResponse httpResponse = router.handle(request);
        if (httpResponse == null) {
            ctx.channel()
                .writeAndFlush(HttpResponses.basic(HttpResponseStatus.NOT_ACCEPTABLE))
                .addListener(ChannelFutureListener.CLOSE);
        } else {
            ctx.channel().writeAndFlush(httpResponse);
        }
    }

}
