package com.tuuzed.tunnel.common.util;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

@SuppressWarnings("Duplicates")
public final class HttpUtils {

    private static final String CRLF = "\r\n";

    @NotNull
    public static Map<String, String> headersOf(@NotNull String... args) {
        if (args.length % 2 != 0) {
            throw new IllegalArgumentException("args.length % 2 != 0");
        }
        Map<String, String> headers = new LinkedHashMap<>(args.length / 2);
        for (int i = 0; i < args.length / 2; i++) {
            headers.put(args[i * 2], args[i * 2 + 1]);
        }
        return headers;
    }


    @NotNull
    public static byte[] httpRequestToBytes(@NotNull HttpRequest request) {
        StringBuilder raw = new StringBuilder();

        HttpMethod method = request.method();
        String uri = request.uri();
        HttpVersion httpVersion = request.protocolVersion();
        HttpHeaders headers = request.headers();
        raw.append(method.name()).append(" ").append(uri).append(" ").append(httpVersion.text()).append(CRLF);
        Iterator<Map.Entry<String, String>> iterator = headers.iteratorAsString();
        while (iterator.hasNext()) {
            Map.Entry<String, String> next = iterator.next();
            raw.append(next.getKey()).append(": ").append(next.getValue()).append(CRLF);
        }
        raw.append(CRLF);
        return raw.toString().getBytes(StandardCharsets.UTF_8);
    }

    @NotNull
    public static ByteBuf httpResponseToBytes(@NotNull HttpResponse response) {
        StringBuilder raw = new StringBuilder();
        HttpVersion httpVersion = response.protocolVersion();
        HttpResponseStatus status = response.status();
        raw.append(httpVersion.text()).append(" ")
            .append(status.code()).append(" ").append(status.reasonPhrase())
            .append(CRLF);
        HttpHeaders headers = response.headers();
        Iterator<Map.Entry<String, String>> iterator = headers.iteratorAsString();
        while (iterator.hasNext()) {
            Map.Entry<String, String> next = iterator.next();
            raw.append(next.getKey()).append(": ").append(next.getValue()).append(CRLF);
        }
        raw.append(CRLF);
        if (response instanceof FullHttpResponse) {
            return Unpooled.wrappedBuffer(raw.toString().getBytes(StandardCharsets.UTF_8))
                .writeBytes(((FullHttpResponse) response).content());
        } else {
            return Unpooled.wrappedBuffer(raw.toString().getBytes(StandardCharsets.UTF_8));
        }
    }


    @Nullable
    public static String getVhost(@NotNull HttpRequest request) {
        final String host = request.headers().get(HttpHeaderNames.HOST);
        return host.split(":")[0];
    }
}
