package com.tuuzed.tunnelweb.framework;

import com.tuuzed.tunnel.log4j.Log4jInitializer;
import com.tuuzed.tunnelweb.HttpRequestHandler;
import com.tuuzed.tunnelweb.HttpRequests;
import com.tuuzed.tunnelweb.HttpResponses;
import com.tuuzed.tunnelweb.HttpWebServer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpResponse;
import org.apache.log4j.Level;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;
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
        new HttpWebServer(bossGroup, workerGroup, null, 8080, 1024 * 1024)
            .routing("/file", new HttpRequestHandler() {
                @Override
                public HttpResponse handle(@NotNull FullHttpRequest request) throws Exception {
                    HttpResponse response = HttpResponses.file(new File("D:\\index.html"));
                    response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/html; charset=utf-8");
                    return response;
                }
            })
            .routing("/json", new HttpRequestHandler() {
                @Override
                public HttpResponse handle(@NotNull FullHttpRequest request) throws Exception {
                    JSONObject json = HttpRequests.contentJsonObject(request);
                    HttpResponse response = HttpResponses.text(json.toString(2));
                    response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/json; charset=utf-8");
                    return response;
                }
            })
            .routing("/form", new HttpRequestHandler() {
                @Override
                public HttpResponse handle(@NotNull FullHttpRequest request) throws Exception {
                    Map<String, List<String>> form = HttpRequests.contentXwwwFormUrlEncoded(request);
                    HttpResponse response = HttpResponses.text(form.toString());
                    response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain; charset=utf-8");
                    return response;
                }
            })
            .start();
        Thread.currentThread().join();
    }
}