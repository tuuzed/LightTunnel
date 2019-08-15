package com.tuuzed.tunnel.cli.server;

import com.tuuzed.tunnel.common.proto.ProtoRequest;
import com.tuuzed.tunnel.server.http.HttpRequestInterceptor;
import io.netty.handler.codec.http.HttpRequest;
import org.jetbrains.annotations.NotNull;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Map;

public final class DefaultHttpRequestInterceptor implements HttpRequestInterceptor {
    @Override
    public void proceed(
        @NotNull SocketAddress localAddress,
        @NotNull SocketAddress remoteAddress,
        @NotNull ProtoRequest protoRequest,
        @NotNull HttpRequest httpRequest
    ) throws Exception {
        handleHeaders(false, localAddress, remoteAddress, protoRequest, httpRequest);
        handleHeaders(true, localAddress, remoteAddress, protoRequest, httpRequest);
    }

    private void handleHeaders(
        boolean setHeaders,
        @NotNull SocketAddress localAddress,
        @NotNull SocketAddress remoteAddress,
        @NotNull ProtoRequest protoRequest,
        @NotNull HttpRequest httpRequest
    ) {

        Map<String, String> headers = setHeaders
            ? protoRequest.setHeaders()
            : protoRequest.addHeaders();

        for (Map.Entry<String, String> it : headers.entrySet()) {
            String name = it.getKey();
            String value = it.getValue();
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
