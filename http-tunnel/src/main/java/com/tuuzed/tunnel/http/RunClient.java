package com.tuuzed.tunnel.http;

import com.tuuzed.tunnel.http.client.TunnelClient;
import io.netty.channel.nio.NioEventLoopGroup;

public class RunClient {
    public static void main(String[] args) throws Exception {
        NioEventLoopGroup workerGroup = new NioEventLoopGroup();
        TunnelClient t1 = new TunnelClient(workerGroup);
        t1.start("127.0.0.1", 5000, "t1", "127.0.0.1", 8080);
        TunnelClient t2 = new TunnelClient(workerGroup);
        t2.start("127.0.0.1", 5000, "t2", "127.0.0.1", 9090);
        System.in.read();
    }

}
