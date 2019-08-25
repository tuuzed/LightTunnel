package com.tuuzed.tunnelweb;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.*;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;

public class HttpResponses {

    @NotNull
    public static FullHttpResponse text(@NotNull String text) {
        DefaultFullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
        byte[] content = text.getBytes(StandardCharsets.UTF_8);
        response.headers().set(HttpHeaderNames.CONTENT_LENGTH, content.length);
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain");
        response.content().writeBytes(content);
        return response;
    }

    @NotNull
    public static FullHttpResponse file(@NotNull File file) {
        DefaultFullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
        try (FileChannel channel = new FileInputStream(file).getChannel()) {
            response.headers().set(HttpHeaderNames.CONTENT_LENGTH, channel.size());
            response.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/octet-stream");
            response.content().writeBytes(channel, 0, (int) channel.size());
        } catch (FileNotFoundException e) {
            return basic(HttpResponseStatus.NOT_FOUND);
        } catch (IOException e) {
            return basic(HttpResponseStatus.INTERNAL_SERVER_ERROR);
        }
        return response;
    }

    @NotNull
    public static FullHttpResponse basic(@NotNull HttpResponseStatus status) {
        return basic(HttpVersion.HTTP_1_1, status);
    }

    @NotNull
    public static FullHttpResponse basic(@NotNull HttpResponseStatus status, @NotNull ByteBuf content) {
        return basic(HttpVersion.HTTP_1_1, status, content);
    }

    @NotNull
    public static FullHttpResponse basic(@NotNull HttpVersion version, @NotNull HttpResponseStatus status) {
        return basic(version, status, Unpooled.wrappedBuffer(status.toString().getBytes()));
    }

    @NotNull
    public static FullHttpResponse basic(@NotNull HttpVersion version, @NotNull HttpResponseStatus status, @NotNull ByteBuf content) {
        FullHttpResponse response = new DefaultFullHttpResponse(version, status);
        response.content().writeBytes(content);
        return response;
    }
}
