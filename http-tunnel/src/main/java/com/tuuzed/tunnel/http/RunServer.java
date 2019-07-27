package com.tuuzed.tunnel.http;

import com.tuuzed.tunnel.http.server.HttpServer;
import com.tuuzed.tunnel.http.server.HttpServerChannels;
import com.tuuzed.tunnel.http.server.TunnelServer;
import com.tuuzed.tunnel.http.server.TunnelServerChannels;
import io.netty.channel.nio.NioEventLoopGroup;

public class RunServer {
    public static void main(String[] args) throws Exception {
        NioEventLoopGroup bossGroup = new NioEventLoopGroup();
        NioEventLoopGroup workerGroup = new NioEventLoopGroup();
        TunnelServerChannels tunnelServerChannels = new TunnelServerChannels();
        HttpServerChannels httpServerChannels = new HttpServerChannels();
        TunnelServer tunnelServer = new TunnelServer(bossGroup, workerGroup, tunnelServerChannels, httpServerChannels);
        HttpServer httpServer = new HttpServer(bossGroup, workerGroup, tunnelServerChannels, httpServerChannels);
        tunnelServer.start(5000);
        httpServer.start(80);
        System.in.read();
    }
}
