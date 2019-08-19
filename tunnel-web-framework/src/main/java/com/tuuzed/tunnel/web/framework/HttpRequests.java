package com.tuuzed.tunnel.web.framework;

import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpRequest;
import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class HttpRequests {
    private static final Logger logger = LoggerFactory.getLogger(HttpRequests.class);

    @NotNull
    public static String path(@NotNull HttpRequest request) {
        final String uri = request.uri();
        return path(uri);
    }

    @NotNull
    public static Map<String, List<String>> query(@NotNull HttpRequest request) {
        final String uri = request.uri();
        final int indexOf = uri.indexOf('?');
        if (indexOf == -1) {
            return Collections.emptyMap();
        } else {
            return resolveKwargs(uri.substring(indexOf + 1));
        }
    }

    @NotNull
    public static JSONObject contentJsonObject(@NotNull FullHttpRequest request) {
        String body = request.content().toString(StandardCharsets.UTF_8);
        return new JSONObject(body);
    }

    @NotNull
    public static JSONArray contentJsonArray(@NotNull FullHttpRequest request) {
        String body = request.content().toString(StandardCharsets.UTF_8);
        return new JSONArray(body);
    }

    @NotNull
    public static Map<String, List<String>> contentXwwwFormUrlEncoded(@NotNull FullHttpRequest request) {
        String content = request.content().toString(StandardCharsets.UTF_8);
        return resolveKwargs(content);
    }

    @NotNull
    private static String path(@NotNull String uri) {
        final int indexOf = uri.indexOf('?');
        if (indexOf == -1) {
            return uri;
        } else {
            return uri.substring(0, indexOf);
        }
    }

    private static Map<String, List<String>> resolveKwargs(String kwargs) {
        if (kwargs == null || kwargs.length() == 0) {
            return Collections.emptyMap();
        }
        Map<String, List<String>> map = new LinkedHashMap<>();
        String[] kvs = kwargs.split("&");
        for (String kv : kvs) {
            String[] kvArray = kv.split("=");
            if (kvArray.length == 2) {
                String k = urldecode(kvArray[0]);
                String v = urldecode(kvArray[1]);
                List<String> vs = map.get(k);
                if (vs == null) {
                    vs = new LinkedList<>();
                }
                vs.add(v);
                map.put(k, vs);
            }
        }
        return map;
    }

    @NotNull
    private static String urldecode(@NotNull String src) {
        try {
            return URLDecoder.decode(src, StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException e) {
            // pass
        }
        return src;
    }
}
