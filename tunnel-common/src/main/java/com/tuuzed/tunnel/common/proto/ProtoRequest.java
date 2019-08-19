package com.tuuzed.tunnel.common.proto;

import com.tuuzed.tunnel.common.proto.internal.ProtoUtils;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

@SuppressWarnings({"Duplicates", "unused"})
public class ProtoRequest {
    private static final String REMOTE_PORT = "$$r";
    private static final String VHOST = "$$v";
    private static final String TOKEN = "$$t";
    // http & https
    private static final String BASIC_AUTH = "$$a";
    private static final String BASIC_AUTH_REALM = "$$r";
    private static final String BASIC_AUTH_USERNAME = "$$u";
    private static final String BASIC_AUTH_PASSWORD = "$$p";
    private static final String REWRITE_HEADERS = "$$rwh";
    private static final String WRITE_HEADERS = "$$wh";

    @NotNull
    private Proto proto;
    @NotNull
    private String localAddr;
    private int localPort;
    @NotNull
    private Map<String, String> options;

    private ProtoRequest(
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
        if (!isHttp() && !isHttps()) {
            throw new NullPointerException(String.format("proto(%s) unsupported", proto));
        }
        final String vhost = option(VHOST);
        if (vhost == null) {
            throw new NullPointerException("vhost == null");
        }
        return vhost;
    }

    @Nullable
    public String token() {
        return option(TOKEN);
    }

    public boolean isEnableBasicAuth() {
        return "1".equals(option(BASIC_AUTH));
    }

    @Nullable
    public String basicAuthRealm() {
        return option(BASIC_AUTH_REALM);
    }

    @Nullable
    public String basicAuthUsername() {
        return option(BASIC_AUTH_USERNAME);
    }

    @Nullable
    public String basicAuthPassword() {
        return option(BASIC_AUTH_PASSWORD);
    }

    @NotNull
    public Map<String, String> rewriteHeaders() {
        return parseHeaders(option(REWRITE_HEADERS));
    }

    @NotNull
    public Map<String, String> writeHeaders() {
        return parseHeaders(option(WRITE_HEADERS));
    }

    @NotNull
    private Map<String, String> parseHeaders(@Nullable String headers) {
        Map<String, String> map = new LinkedHashMap<>();
        if (headers != null) {
            for (String it : headers.split(";")) {
                String[] line = it.split(":");
                if (line.length == 2) {
                    map.put(line[0], line[1]);
                }
            }
        }
        return map;
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
    public static Builder tcpBuilder(int remotePort) {
        return new Builder(Proto.TCP).setOptionInternal(REMOTE_PORT, String.valueOf(remotePort));
    }

    @NotNull
    public Builder cloneTcpBuilder() {
        return cloneTcpBuilder(remotePort());
    }

    @NotNull
    public Builder cloneTcpBuilder(int remotePort) {
        Builder builder = new Builder(Proto.TCP);
        builder.localAddr = localAddr;
        builder.localPort = localPort;
        builder.options = options;
        builder.setOptionInternal(REMOTE_PORT, String.valueOf(remotePort));
        return builder;
    }

    @NotNull
    public static Builder httpBuilder(@NotNull String vhost) {
        return new Builder(Proto.HTTP).setOptionInternal(VHOST, vhost);
    }

    @NotNull
    public Builder cloneHttpBuilder() {
        return cloneHttpBuilder(vhost());
    }

    @NotNull
    public Builder cloneHttpBuilder(@NotNull String vhost) {
        Builder builder = new Builder(Proto.HTTP);
        builder.localAddr = localAddr;
        builder.localPort = localPort;
        builder.options = options;
        builder.setOptionInternal(VHOST, vhost);
        return builder;
    }

    @NotNull
    public static Builder httpsBuilder(@NotNull String vhost) {
        return new Builder(Proto.HTTPS).setOptionInternal(VHOST, vhost);
    }

    @NotNull
    public Builder cloneHttpsBuilder() {
        return cloneHttpBuilder(vhost());
    }

    @NotNull
    public Builder cloneHttpsBuilder(@NotNull String vhost) {
        Builder builder = new Builder(Proto.HTTPS);
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
                return String.format("[%s:%d<-tcp://{server}:%d <%s>]", localAddr, localPort, remotePort(), ProtoUtils.map2String(options));
            case HTTP:
                return String.format("[%s:%d<-http://%s <%s>]", localAddr, localPort, vhost(), ProtoUtils.map2String(options));
            case HTTPS:
                return String.format("[%s:%d<-https://%s <%s>]", localAddr, localPort, vhost(), ProtoUtils.map2String(options));
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


    public static class Builder {
        private Proto proto;
        private String localAddr;
        private int localPort;
        private Map<String, String> options;

        private Builder(@NotNull Proto proto) {
            this.proto = proto;
        }

        @NotNull
        public Builder setLocalAddr(@NotNull String localAddr) {
            this.localAddr = localAddr;
            return this;
        }

        @NotNull
        public Builder setLocalPort(int localPort) {
            this.localPort = localPort;
            return this;
        }

        @NotNull
        public Builder setToken(@NotNull String token) {
            setOptionInternal(TOKEN, token);
            return this;
        }

        @NotNull
        public Builder setBasicAuth(boolean enable, @NotNull String realm) {
            setOptionInternal(BASIC_AUTH, enable ? "1" : "0");
            setOptionInternal(BASIC_AUTH_REALM, realm);
            return this;
        }

        @NotNull
        public Builder setBasicAuthAccount(@NotNull String username, @NotNull String password) {
            setOptionInternal(BASIC_AUTH_USERNAME, username);
            setOptionInternal(BASIC_AUTH_PASSWORD, password);
            return this;
        }

        @NotNull
        public Builder setWriteHeaders(@NotNull Map<String, String> headers) {
            setHeadersOption(WRITE_HEADERS, headers);
            return this;
        }

        @NotNull
        public Builder setRewriteHeaders(@NotNull Map<String, String> headers) {
            setHeadersOption(REWRITE_HEADERS, headers);
            return this;
        }

        private void setHeadersOption(@NotNull String option, @NotNull Map<String, String> headers) {
            boolean isFirst = true;
            StringBuilder sb = new StringBuilder();
            for (Map.Entry<String, String> it : headers.entrySet()) {
                if (!isFirst) {
                    sb.append(";");
                }
                sb.append(it.getKey()).append(":").append(it.getValue());
                isFirst = false;
            }
            setOptionInternal(option, sb.toString());
        }

        @NotNull
        private Builder setOptionInternal(@NotNull String key, @NotNull String value) {
            if (options == null) {
                options = new LinkedHashMap<>();
            }
            options.put(key, value);
            return this;
        }

        @NotNull
        public Builder setOption(@NotNull String key, @NotNull String value) {
            if (key.startsWith("$$")) {
                throw new IllegalArgumentException("$$打头的key为系统保留的key");
            }
            setOptionInternal(key, value);
            return this;
        }

        @NotNull
        public ProtoRequest build() {
            if (localAddr == null) {
                throw new IllegalArgumentException("localAddr == null");
            }
            if (localPort < 0 || localPort > 65535) {
                throw new IllegalArgumentException("localPort < 0 || localPort > 65535");
            }
            if (options == null) {
                options = Collections.emptyMap();
            }
            switch (proto) {
                case UNKNOWN:
                    break;
                case TCP:
                    if (!options.containsKey(REMOTE_PORT)) {
                        throw new IllegalArgumentException("TCP协议必须设置REMOTE_PORT");
                    }
                    break;
                case HTTP:
                    if (!options.containsKey(VHOST)) {
                        throw new IllegalArgumentException("HTTP协议必须设置VHOST");
                    }
                    break;
                default:
                    break;
            }
            return new ProtoRequest(proto, localAddr, localPort, options);
        }

    }
}
