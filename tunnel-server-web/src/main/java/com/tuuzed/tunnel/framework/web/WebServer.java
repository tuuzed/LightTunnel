package com.tuuzed.tunnel.framework.web;

import com.tuuzed.tunnel.common.log4j.Log4jInitializer;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.HttpContentCompressor;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;
import org.apache.log4j.Level;

public class WebServer {

    public static void main(String[] args) throws InterruptedException {
        Log4jInitializer.initializeThirdLibrary(Level.WARN);
        Log4jInitializer.builder().setLevel(Level.ALL).initialize();
        new WebServer().start();
    }

    public void start() throws InterruptedException {
        ServerBootstrap bootstrap = new ServerBootstrap();
        Channel ch = bootstrap.group(new NioEventLoopGroup(1), new NioEventLoopGroup())
            .channel(NioServerSocketChannel.class)
            .childHandler(new ChannelInitializer<NioSocketChannel>() {
                @Override
                protected void initChannel(NioSocketChannel ch) throws Exception {
                    ch.pipeline()
                        .addLast("decoder", new HttpRequestDecoder())
                        .addLast("encoder", new HttpResponseEncoder())
                        .addLast("aggregator", new HttpObjectAggregator(1024 * 128))
                        .addLast("deflater", new HttpContentCompressor())
                        .addLast("dispatcher", new HttpRequestDispatcher());
                }
            }).bind("0.0.0.0", 8080).channel();
        ch.closeFuture().sync();
    }

}
