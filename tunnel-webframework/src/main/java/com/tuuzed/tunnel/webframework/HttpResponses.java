package com.tuuzed.tunnel.webframework;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.*;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.channels.FileChannel;

public class HttpResponses {

    @NotNull
    public static HttpResponse file(@NotNull File file) {
        DefaultFullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
        try (FileChannel channel = new FileInputStream(file).getChannel()) {
            response.headers().set(HttpHeaderNames.CONTENT_LENGTH, channel.size());
            response.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/octet-stream");
            response.content().writeBytes(channel, 0, (int) channel.size());
        } catch (FileNotFoundException e) {
            return raw(HttpResponseStatus.NOT_FOUND);
        } catch (IOException e) {
            return raw(HttpResponseStatus.INTERNAL_SERVER_ERROR);
        }
        return response;
    }


    @NotNull
    public static HttpResponse raw(
        @NotNull HttpResponseStatus status
    ) {
        return raw(HttpVersion.HTTP_1_1, status);
    }

    @NotNull
    public static HttpResponse raw(
        @NotNull HttpResponseStatus status,
        @NotNull ByteBuf content
    ) {
        return raw(HttpVersion.HTTP_1_1, status, content);
    }

    @NotNull
    public static HttpResponse raw(
        @NotNull HttpVersion version,
        @NotNull HttpResponseStatus status
    ) {
        return raw(version, status, Unpooled.wrappedBuffer(status.toString().getBytes()));
    }

    @NotNull
    public static HttpResponse raw(
        @NotNull HttpVersion version,
        @NotNull HttpResponseStatus status,
        @NotNull ByteBuf content
    ) {
        return new DefaultFullHttpResponse(version, status, content);
    }
}
