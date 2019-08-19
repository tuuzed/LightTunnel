package com.tuuzed.tunnel.web.framework;

import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Pattern;

@SuppressWarnings("Duplicates")
public class HttpRouter implements HttpRequestHandler {

    @NotNull
    private final List<Entry> beforeHandlers = new CopyOnWriteArrayList<>();
    private final List<Entry> routingHandlers = new CopyOnWriteArrayList<>();

    public void before(@NotNull String uriRegex, int priority, @NotNull HttpRequestHandler handler) {
        beforeHandlers.add(new Entry(Pattern.compile(uriRegex), priority, handler));
        Collections.sort(beforeHandlers);
    }

    public void routing(@NotNull String uriRegex, int priority, @NotNull HttpRequestHandler handler) {
        routingHandlers.add(new Entry(Pattern.compile(uriRegex), priority, handler));
        Collections.sort(routingHandlers);
    }

    @Nullable
    @Override
    public final HttpResponse handle(@NotNull FullHttpRequest request) throws Exception {
        HttpRequestHandler handler = null;

        for (Entry it : beforeHandlers) {
            handler = it.matchHandler(request);
            if (handler == null) {
                break;
            }
        }
        if (handler != null) {
            HttpResponse httpResponse = handler.handle(request);
            if (httpResponse != null) {
                return httpResponse;
            }
        }

        for (Entry it : routingHandlers) {
            handler = it.matchHandler(request);
            if (handler == null) {
                break;
            }
        }
        if (handler != null) {
            return handler.handle(request);
        } else {
            return HttpResponses.basic(HttpResponseStatus.NOT_FOUND);
        }

    }


    private static class Entry implements Comparable<Entry> {
        private final Pattern pattern;
        private final int priority;
        private final HttpRequestHandler handler;

        private Entry(Pattern pattern, int priority, HttpRequestHandler handler) {
            this.pattern = pattern;
            this.priority = priority;
            this.handler = handler;
        }

        @Nullable
        private HttpRequestHandler matchHandler(@NotNull FullHttpRequest request) {
            if (pattern.matcher(HttpRequests.path(request)).find()) {
                return handler;
            }
            return null;
        }

        @Override
        public int compareTo(@NotNull Entry o) {
            // 倒序（大 -> 小）
            return Integer.compare(o.priority, this.priority);
        }
    }
}
