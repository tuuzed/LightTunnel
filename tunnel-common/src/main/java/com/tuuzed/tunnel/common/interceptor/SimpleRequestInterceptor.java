package com.tuuzed.tunnel.common.interceptor;

import com.tuuzed.tunnel.common.proto.ProtoException;
import com.tuuzed.tunnel.common.proto.ProtoRequest;
import com.tuuzed.tunnel.common.util.HttpUtils;
import com.tuuzed.tunnel.common.util.PortUtils;
import io.netty.handler.codec.http.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;

public final class SimpleRequestInterceptor implements ProtoRequestInterceptor, HttpRequestInterceptor {
    @NotNull
    private final String presetToken;
    @Nullable
    private final String portRule;

    public SimpleRequestInterceptor(@NotNull String token, @Nullable String portRule) {
        this.portRule = portRule;
        this.presetToken = token;
    }

    @NotNull
    @Override
    public ProtoRequest handleProtoRequest(@NotNull ProtoRequest request) throws ProtoException {
        String token = request.token();
        if (!presetToken.equals(token)) {
            throw new ProtoException(String.format("request(%s), Bad Token(%s)", request.toString(), token));
        }
        if (request.isTcp()) {
            final int remotePort = request.remotePort();
            if (portRule != null && !PortUtils.inPortRule(portRule, remotePort)) {
                throw new ProtoException(String.format("request(%s), remotePort(%s) Not allowed to use.", request.toString(), remotePort));
            }
            return request;
        }
        return request;
    }

    @Nullable
    @Override
    public HttpResponse handleHttpRequest(
        @NotNull SocketAddress localAddress,
        @NotNull SocketAddress remoteAddress,
        @NotNull ProtoRequest protoRequest,
        @NotNull HttpRequest httpRequest
    ) {
        handleHttpHeaders(false, localAddress, remoteAddress, protoRequest, httpRequest);
        handleHttpHeaders(true, localAddress, remoteAddress, protoRequest, httpRequest);
        if (protoRequest.isEnableBasicAuth()) {
            final String authorization = httpRequest.headers().get(HttpHeaderNames.AUTHORIZATION);
            final String[] account = HttpUtils.parseBasicAuthorization(authorization);
            final String username = protoRequest.basicAuthUsername();
            final String password = protoRequest.basicAuthPassword();
            if (account == null ||
                username == null || password == null ||
                !username.equals(account[0]) || !password.equals(account[1])
            ) {
                FullHttpResponse httpResponse = new DefaultFullHttpResponse(
                    httpRequest.protocolVersion(),
                    HttpResponseStatus.UNAUTHORIZED
                );
                byte[] content = "UNAUTHORIZED".getBytes(StandardCharsets.UTF_8);
                httpResponse.headers().add(HttpHeaderNames.SERVER, "Tunnel/x");
                httpResponse.headers().add(HttpHeaderNames.WWW_AUTHENTICATE, String.format("Basic realm=\"%s\"", protoRequest.basicAuthRealm()));
                httpResponse.headers().add(HttpHeaderNames.CONNECTION, "keep-alive");
                httpResponse.headers().add(HttpHeaderNames.ACCEPT_RANGES, "bytes");

                httpResponse.headers().add(HttpHeaderNames.DATE, new Date().toString());
                httpResponse.headers().add(HttpHeaderNames.CONTENT_LENGTH, content.length);
                httpResponse.content().writeBytes(content);
                return httpResponse;
            }
        }
        return null;
    }


    private void handleHttpHeaders(
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
