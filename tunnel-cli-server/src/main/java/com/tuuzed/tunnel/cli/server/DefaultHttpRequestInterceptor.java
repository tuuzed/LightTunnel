package com.tuuzed.tunnel.cli.server;

import com.tuuzed.tunnel.common.proto.ProtoRequest;
import com.tuuzed.tunnel.server.http.HttpRequestInterceptor;
import io.netty.handler.codec.http.HttpRequest;
import org.jetbrains.annotations.NotNull;

import java.net.InetSocketAddress;
import java.net.SocketAddress;

public final class DefaultHttpRequestInterceptor implements HttpRequestInterceptor {
    @Override
    public void proceed(
        @NotNull SocketAddress localAddress,
        @NotNull SocketAddress remoteAddress,
        @NotNull ProtoRequest protoRequest,
        @NotNull HttpRequest httpRequest
    ) throws Exception {

        handleSetHeaders(localAddress, remoteAddress, protoRequest, httpRequest);
        handleAddHeaders(localAddress, remoteAddress, protoRequest, httpRequest);
    }

    private void handleAddHeaders(SocketAddress localAddress, SocketAddress remoteAddress, ProtoRequest protoRequest, HttpRequest httpRequest) {
        handleHeaders(false, localAddress, remoteAddress, protoRequest, httpRequest);
    }

    private void handleSetHeaders(SocketAddress localAddress, SocketAddress remoteAddress, ProtoRequest protoRequest, HttpRequest httpRequest) {
        handleHeaders(true, localAddress, remoteAddress, protoRequest, httpRequest);
    }


    private void handleHeaders(
        boolean setHeaders,
        @NotNull SocketAddress localAddress,
        @NotNull SocketAddress remoteAddress,
        @NotNull ProtoRequest protoRequest,
        @NotNull HttpRequest httpRequest
    ) {

        String line = setHeaders
            ? protoRequest.option("set_headers")
            : protoRequest.option("add_headers");

        if (line == null) {
            return;
        }
        String[] headers = line.split(";");
        for (String header : headers) {
            String[] kv = header.split(":");
            if (kv.length == 2) {
                String name = kv[0];
                String value = kv[1];
                if ("$remote_addr".equals(value)) {
                    if (remoteAddress instanceof InetSocketAddress) {
                        if (setHeaders) {
                            httpRequest.headers().remove(name);
                        }
                        httpRequest.headers().add(name, ((InetSocketAddress) remoteAddress).getAddress().toString());
                    }
                } else {
                    if (setHeaders) {
                        httpRequest.headers().remove(name);
                    }
                    httpRequest.headers().add(name, value);
                }
            }
        }
    }


}
