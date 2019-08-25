package com.tuuzed.tunnel.interceptor;

import com.tuuzed.tunnel.TunnelVersion;
import com.tuuzed.tunnel.proto.ProtoException;
import com.tuuzed.tunnel.proto.ProtoRequest;
import com.tuuzed.tunnel.util.HttpUtils;
import com.tuuzed.tunnel.util.PortUtils;
import io.netty.handler.codec.http.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;

public final class SimpleRequestInterceptor implements ProtoRequestInterceptor, HttpRequestInterceptor {
    /**
     * remote_addr 模板字符串
     */
    private static final String TL_REMOTE_ADDR = "$remote_addr";
    /**
     * 预置Token
     */
    @NotNull
    private final String presetToken;
    /**
     * 端口白名单
     */
    @Nullable
    private final String allowPorts;

    public SimpleRequestInterceptor(@NotNull String token, @Nullable String allowPorts) {
        this.presetToken = token;
        this.allowPorts = allowPorts;
    }

    @NotNull
    @Override
    public ProtoRequest handleProtoRequest(@NotNull ProtoRequest request) throws ProtoException {
        verifyToken(request);
        if (request.isTcp()) {
            final int remotePort = request.remotePort();
            if (allowPorts != null && !PortUtils.inPortRule(allowPorts, remotePort)) {
                throw new ProtoException(String.format("request(%s), remotePort(%s) Not allowed to use.", request.toString(), remotePort));
            }
            return request;
        }
        return request;
    }

    /**
     * 验证Token
     */
    private void verifyToken(@NotNull ProtoRequest request) throws ProtoException {
        final String token = request.token();
        if (!presetToken.equals(token)) {
            throw new ProtoException(String.format("request(%s), Bad Token(%s)", request.toString(), token));
        }
    }

    @Nullable
    @Override
    public HttpResponse handleHttpRequest(
        @NotNull SocketAddress localAddress,
        @NotNull SocketAddress remoteAddress,
        @NotNull ProtoRequest protoRequest,
        @NotNull HttpRequest httpRequest
    ) {
        handleRewriteHttpHeaders(localAddress, remoteAddress, protoRequest, httpRequest);
        handleWriteHttpHeaders(localAddress, remoteAddress, protoRequest, httpRequest);
        if (protoRequest.isEnableBasicAuth()) {
            return handleHttpBasicAuth(protoRequest, httpRequest);
        }
        return null;
    }

    @Nullable
    private HttpResponse handleHttpBasicAuth(@NotNull ProtoRequest protoRequest, @NotNull HttpRequest httpRequest) {
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
            final byte[] content = HttpResponseStatus.UNAUTHORIZED.toString().getBytes(StandardCharsets.UTF_8);
            httpResponse.headers().add(HttpHeaderNames.SERVER, "Tunnel/" + TunnelVersion.VERSION_NAME);
            httpResponse.headers().add(HttpHeaderNames.WWW_AUTHENTICATE, String.format("Basic realm=\"%s\"", protoRequest.basicAuthRealm()));
            httpResponse.headers().add(HttpHeaderNames.CONNECTION, "keep-alive");
            httpResponse.headers().add(HttpHeaderNames.ACCEPT_RANGES, "bytes");

            httpResponse.headers().add(HttpHeaderNames.DATE, new Date().toString());
            httpResponse.headers().add(HttpHeaderNames.CONTENT_LENGTH, content.length);
            httpResponse.content().writeBytes(content);
            return httpResponse;
        }
        return null;
    }

    private void handleRewriteHttpHeaders(
        @NotNull SocketAddress localAddress,
        @NotNull SocketAddress remoteAddress,
        @NotNull ProtoRequest protoRequest,
        @NotNull HttpRequest httpRequest
    ) {
        handleHttpHeaders(true, localAddress, remoteAddress, protoRequest, httpRequest);
    }

    private void handleWriteHttpHeaders(
        @NotNull SocketAddress localAddress,
        @NotNull SocketAddress remoteAddress,
        @NotNull ProtoRequest protoRequest,
        @NotNull HttpRequest httpRequest
    ) {
        handleHttpHeaders(false, localAddress, remoteAddress, protoRequest, httpRequest);
    }

    private void handleHttpHeaders(
        boolean rewirte,
        @NotNull SocketAddress localAddress,
        @NotNull SocketAddress remoteAddress,
        @NotNull ProtoRequest protoRequest,
        @NotNull HttpRequest httpRequest
    ) {

        Map<String, String> headers = rewirte
            ? protoRequest.rewriteHeaders()
            : protoRequest.writeHeaders();
        if (headers.isEmpty()) {
            return;
        }
        for (Map.Entry<String, String> it : headers.entrySet()) {
            final String name = it.getKey();
            final String value = it.getValue();
            if (TL_REMOTE_ADDR.equals(value)) {
                if (remoteAddress instanceof InetSocketAddress) {
                    final String remoteAddr = ((InetSocketAddress) remoteAddress).getAddress().toString();
                    if (rewirte && httpRequest.headers().contains(name)) {
                        httpRequest.headers().set(name, remoteAddr);
                    } else {
                        httpRequest.headers().add(name, remoteAddr);
                    }
                }
            } else {
                if (rewirte && httpRequest.headers().contains(name)) {
                    httpRequest.headers().set(name, value);
                } else {
                    httpRequest.headers().add(name, value);
                }
            }
        }
    }

}
