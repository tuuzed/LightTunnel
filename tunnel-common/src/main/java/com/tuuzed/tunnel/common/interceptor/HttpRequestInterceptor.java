package com.tuuzed.tunnel.common.interceptor;

import com.tuuzed.tunnel.common.proto.ProtoRequest;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.SocketAddress;

public interface HttpRequestInterceptor {

    HttpRequestInterceptor DEFAULT = new HttpRequestInterceptor() {
        @Nullable
        @Override
        public HttpResponse handleHttpRequest(@NotNull SocketAddress localAddress, @NotNull SocketAddress remoteAddress, @NotNull ProtoRequest protoRequest, @NotNull HttpRequest httpRequest) {
            return null;
        }
    };

    @Nullable
    HttpResponse handleHttpRequest(
        @NotNull SocketAddress localAddress,
        @NotNull SocketAddress remoteAddress,
        @NotNull ProtoRequest protoRequest,
        @NotNull HttpRequest httpRequest
    );

}
