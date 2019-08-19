package com.tuuzed.tunnel.webframework;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpContentCompressor;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;
import org.jetbrains.annotations.NotNull;

public class HttpWebServerChannelInitializer extends ChannelInitializer<SocketChannel> {

    private final int maxContentLength;

    @NotNull
    private final HttpRouter router;

    public HttpWebServerChannelInitializer(int maxContentLength, @NotNull HttpRouter router) {
        this.maxContentLength = maxContentLength;
        this.router = router;
    }

    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        ch.pipeline()
            .addLast("decoder", new HttpRequestDecoder())
            .addLast("encoder", new HttpResponseEncoder())
            .addLast("aggregator", new HttpObjectAggregator(maxContentLength))
            .addLast("deflater", new HttpContentCompressor())
            .addLast("dispatcher", new HttpRequestDispatcher(router));
    }
}
