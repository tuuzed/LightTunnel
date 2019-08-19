package com.tuuzed.tunnel.web.framework;

import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface HttpRequestHandler {

    @Nullable
    HttpResponse handle(@NotNull FullHttpRequest request) throws Exception;

}
