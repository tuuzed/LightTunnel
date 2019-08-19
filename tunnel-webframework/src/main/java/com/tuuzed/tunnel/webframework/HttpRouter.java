package com.tuuzed.tunnel.webframework;

import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class HttpRouter implements HttpRequestHandler {

    @NotNull
    private final Map<String, HttpRequestHandler> routingTable = new ConcurrentHashMap<>();

    @NotNull
    public HttpRouter routing(@NotNull String path, @NotNull HttpRequestHandler handler) {
        if (routingTable.containsKey(path)) {
            throw new IllegalStateException(String.format("path: (%s) already exists", path));
        }
        routingTable.put(path, handler);
        return this;
    }

    @Nullable
    @Override
    public final HttpResponse handle(@NotNull FullHttpRequest request) throws Exception {
        HttpRequestHandler handler = getHandler(request);
        if (handler != null) {
            return handler.handle(request);
        } else {
            return HttpResponses.raw(HttpResponseStatus.NOT_FOUND);
        }
    }

    @Nullable
    private HttpRequestHandler getHandler(@NotNull FullHttpRequest request) {
        return routingTable.get(HttpRequests.path(request));
    }

}
