package com.tuuzed.tunnel.webframework;

import io.netty.handler.codec.http.HttpRequest;
import org.jetbrains.annotations.NotNull;
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
    static String path(@NotNull String uri) {
        final int indexOf = uri.indexOf('?');
        if (indexOf == -1) {
            return uri;
        } else {
            return uri.substring(0, indexOf);
        }
    }

    @NotNull
    public static Map<String, List<String>> query(@NotNull HttpRequest request) {
        final String uri = request.uri();
        return query(uri);
    }

    @NotNull
    static Map<String, List<String>> query(String uri) {
        final int indexOf = uri.indexOf('?');
        if (indexOf == -1) {
            return Collections.emptyMap();
        } else {
            Map<String, List<String>> queryMap = new LinkedHashMap<>();
            String queryString = uri.substring(indexOf + 1);
            try {
                queryString = URLDecoder.decode(queryString, StandardCharsets.UTF_8.name());
            } catch (UnsupportedEncodingException e) {
                // pass
            }
            String[] kvs = queryString.split("&");
            for (String kv : kvs) {
                String[] kvArray = kv.split("=");
                if (kvArray.length == 2) {
                    String k = kvArray[0];
                    String v = kvArray[1];
                    List<String> vs = queryMap.get(k);
                    if (vs == null) {
                        vs = new LinkedList<>();
                    }
                    vs.add(v);
                    queryMap.put(k, vs);
                }
            }
            logger.trace("queryString: {}", queryString);
            return queryMap;
        }
    }
}
