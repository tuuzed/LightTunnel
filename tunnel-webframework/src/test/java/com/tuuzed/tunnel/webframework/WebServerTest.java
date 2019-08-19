package com.tuuzed.tunnel.webframework;

import com.tuuzed.tunnel.common.log4j.Log4jInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpResponse;
import org.apache.log4j.Level;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import java.io.File;
import java.util.List;
import java.util.Map;

public class WebServerTest {

    @Test
    public void start() throws Exception {
        Log4jInitializer.initializeThirdLibrary(Level.WARN);
        Log4jInitializer.builder().setLevel(Level.ALL).initialize();
        NioEventLoopGroup bossGroup = new NioEventLoopGroup(1);
        NioEventLoopGroup workerGroup = new NioEventLoopGroup(Runtime.getRuntime().availableProcessors() + 1);
        new HttpWebServer(bossGroup, workerGroup)
            .routing("/", new HttpRequestHandler() {
                @Override
                public HttpResponse handle(@NotNull FullHttpRequest request) throws Exception {
                    Map<String, List<String>> query = HttpRequests.query(request);
                    HttpResponse response = HttpResponses.file(new File("D:\\index.html"));
                    response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/html; charset=utf-8");
                    return response;
                }
            })
            .start();
        Thread.currentThread().join();
    }
}