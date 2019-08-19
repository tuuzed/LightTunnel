package com.tuuzed.tunnel.webframework.web;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class HttpRequestDispatcher extends SimpleChannelInboundHandler<HttpObject> {

    public static final Logger logger = LoggerFactory.getLogger(HttpRequestDispatcher.class);

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, HttpObject msg) throws Exception {
        FullHttpRequest request = (FullHttpRequest) msg;
        logger.trace("request: " + request);
    }


}
