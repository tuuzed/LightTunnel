package com.tuuzed.tunnel.server.http.internal;

import io.netty.handler.codec.http.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Map;

public final class HttpRequestUtils {
    private static final String CRLF = "\r\n";

    @NotNull
    public static byte[] httpRequest2Bytes(@NotNull HttpRequest request) {
        HttpMethod method = request.method();
        String uri = request.uri();
        HttpVersion httpVersion = request.protocolVersion();
        HttpHeaders headers = request.headers();
        StringBuilder raw = new StringBuilder();
        raw.append(method.name()).append(" ").append(uri).append(" ").append(httpVersion.text()).append(CRLF);
        Iterator<Map.Entry<String, String>> iterator = headers.iteratorAsString();
        while (iterator.hasNext()) {
            Map.Entry<String, String> next = iterator.next();
            raw.append(next.getKey()).append(": ").append(next.getValue()).append(CRLF);
        }
        raw.append("\r\n");
        return raw.toString().getBytes(StandardCharsets.UTF_8);
    }

    @Nullable
    public static String getVhost(@NotNull HttpRequest request) {
        final String host = request.headers().get(HttpHeaderNames.HOST);
        return host.split(":")[0];
    }

}
