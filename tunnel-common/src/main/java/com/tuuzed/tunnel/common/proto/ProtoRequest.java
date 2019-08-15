package com.tuuzed.tunnel.common.proto;

import com.tuuzed.tunnel.common.proto.internal.ProtoUtils;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;

@SuppressWarnings("Duplicates")
public class ProtoRequest {
    static final String REMOTE_PORT = "$r";
    static final String VHOST = "$v";

    @NotNull
    private Proto proto;
    @NotNull
    private String localAddr;
    private int localPort;
    @NotNull
    private Map<String, String> options;

    ProtoRequest(
        @NotNull Proto proto,
        @NotNull String localAddr,
        int localPort,
        @NotNull Map<String, String> options
    ) {
        this.proto = proto;
        this.localAddr = localAddr;
        this.localPort = localPort;
        this.options = options;
    }

    @NotNull
    public Proto proto() {
        return proto;
    }

    @NotNull
    public String localAddr() {
        return localAddr;
    }

    public int localPort() {
        return localPort;
    }

    @Nullable
    public String option(@NotNull String key) {
        return options.get(key);
    }

    public int remotePort() {
        final String remotePort = option(REMOTE_PORT);
        if (remotePort == null) {
            throw new NullPointerException("remotePort == null");
        }
        return Integer.parseInt(remotePort);
    }


    @NotNull
    public String vhost() {
        final String vhost = option(VHOST);
        if (vhost == null) {
            throw new NullPointerException("vhost == null");
        }
        return vhost;
    }


    public boolean isTcp() {
        return proto == Proto.TCP;
    }

    public boolean isHttp() {
        return proto == Proto.HTTP;
    }

    public boolean isHttps() {
        return proto == Proto.HTTPS;
    }


    @NotNull
    public static ProtoRequest fromBytes(@NotNull byte[] bytes) throws ProtoException {
        final ByteBuf buffer = Unpooled.wrappedBuffer(bytes);
        try {
            Proto proto = Proto.of(buffer.readByte());
            int localPort = buffer.readInt();

            byte[] loadAddrBytes = new byte[buffer.readInt()];
            buffer.readBytes(loadAddrBytes);
            String loadAddr = new String(loadAddrBytes, StandardCharsets.UTF_8);

            byte[] optionsBytes = new byte[buffer.readInt()];
            buffer.readBytes(optionsBytes);
            Map<String, String> options = ProtoUtils.string2Map(new String(optionsBytes, StandardCharsets.UTF_8));

            return new ProtoRequest(proto, loadAddr, localPort, options);
        } catch (Exception e) {
            throw new ProtoException("解析失败，数据异常", e);
        } finally {
            buffer.release();
        }
    }

    @NotNull
    public byte[] toBytes() {
        final ByteBuf buffer = Unpooled.buffer();
        buffer.writeByte(proto.value());
        buffer.writeInt(localPort);

        final byte[] loadAddrBytes = localAddr.getBytes(StandardCharsets.UTF_8);
        buffer.writeInt(loadAddrBytes.length);
        buffer.writeBytes(loadAddrBytes);

        final byte[] optionsBytes = ProtoUtils.map2String(options).getBytes(StandardCharsets.UTF_8);
        buffer.writeInt(optionsBytes.length);
        buffer.writeBytes(optionsBytes);

        final byte[] bytes = new byte[buffer.readableBytes()];
        buffer.readBytes(bytes);

        buffer.release();
        return bytes;
    }

    @NotNull
    public static ProtoRequestBuilder tcpBuilder(int remotePort) {
        return new ProtoRequestBuilder(Proto.TCP).setOptionInternal(REMOTE_PORT, String.valueOf(remotePort));
    }

    @NotNull
    public static ProtoRequestBuilder httpBuilder(@NotNull String vhost) {
        return new ProtoRequestBuilder(Proto.HTTP).setOptionInternal(VHOST, vhost);
    }

    @NotNull
    public static ProtoRequestBuilder httpsBuilder(@NotNull String vhost) {
        return new ProtoRequestBuilder(Proto.HTTPS).setOptionInternal(VHOST, vhost);
    }

    @NotNull
    public ProtoRequestBuilder cloneTcpBuilder() {
        return cloneTcpBuilder(remotePort());
    }

    @NotNull
    public ProtoRequestBuilder cloneTcpBuilder(int remotePort) {
        ProtoRequestBuilder builder = new ProtoRequestBuilder(Proto.TCP);
        builder.localAddr = localAddr;
        builder.localPort = localPort;
        builder.options = options;
        builder.setOptionInternal(REMOTE_PORT, String.valueOf(remotePort));
        return builder;
    }

    @NotNull
    public ProtoRequestBuilder cloneHttpBuilder() {
        return cloneHttpBuilder(vhost());
    }

    @NotNull
    public ProtoRequestBuilder cloneHttpBuilder(@NotNull String vhost) {
        ProtoRequestBuilder builder = new ProtoRequestBuilder(Proto.HTTP);
        builder.localAddr = localAddr;
        builder.localPort = localPort;
        builder.options = options;
        builder.setOptionInternal(VHOST, vhost);
        return builder;
    }

    @NotNull
    public ProtoRequestBuilder cloneHttpsBuilder() {
        return cloneHttpBuilder(vhost());
    }

    @NotNull
    public ProtoRequestBuilder cloneHttpsBuilder(@NotNull String vhost) {
        ProtoRequestBuilder builder = new ProtoRequestBuilder(Proto.HTTPS);
        builder.localAddr = localAddr;
        builder.localPort = localPort;
        builder.options = options;
        builder.setOptionInternal(VHOST, vhost);
        return builder;
    }

    @Override
    public String toString() {
        switch (proto) {
            case TCP:
                return String.format("[%s:%d<-tcp://tunnel.server:%d?%s]", localAddr, localPort, remotePort(), ProtoUtils.map2String(options));
            case HTTP:
                return String.format("[%s:%d<-http://%s?%s]", localAddr, localPort, vhost(), ProtoUtils.map2String(options));
            case HTTPS:
                return String.format("[%s:%d<-https://%s?%s]", localAddr, localPort, vhost(), ProtoUtils.map2String(options));
            default:
                return "";
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ProtoRequest that = (ProtoRequest) o;
        return localPort == that.localPort &&
            proto == that.proto &&
            localAddr.equals(that.localAddr) &&
            options.equals(that.options);
    }

    @Override
    public int hashCode() {
        return Objects.hash(proto, localAddr, localPort, options);
    }


}
