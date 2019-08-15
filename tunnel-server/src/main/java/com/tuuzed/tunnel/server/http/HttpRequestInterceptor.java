package com.tuuzed.tunnel.server.http;

import com.tuuzed.tunnel.common.proto.ProtoRequest;
import io.netty.handler.codec.http.HttpRequest;
import org.jetbrains.annotations.NotNull;

import java.net.SocketAddress;

public interface HttpRequestInterceptor {
    HttpRequestInterceptor DEFAULT = new HttpRequestInterceptor() {
        @Override
        public void proceed(
            @NotNull SocketAddress localAddress,
            @NotNull SocketAddress remoteAddress,
            @NotNull ProtoRequest protoRequest,
            @NotNull HttpRequest httpRequest
        ) throws Exception {
        }
    };

    void proceed(
        @NotNull SocketAddress localAddress,
        @NotNull SocketAddress remoteAddress,
        @NotNull ProtoRequest protoRequest,
        @NotNull HttpRequest httpRequest
    ) throws Exception;
}
