package com.tuuzed.tunnel.common.proto;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.util.*;

public class ProtoRequest {
    private static final String REMOTE_PORT = "$r";
    private static final String VHOST = "$v";

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
            Map<String, String> options = string2Map(new String(optionsBytes, StandardCharsets.UTF_8));

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
        buffer.writeByte(proto.value);
        buffer.writeInt(localPort);

        final byte[] loadAddrBytes = localAddr.getBytes(StandardCharsets.UTF_8);
        buffer.writeInt(loadAddrBytes.length);
        buffer.writeBytes(loadAddrBytes);

        final byte[] optionsBytes = map2String(options).getBytes(StandardCharsets.UTF_8);
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
    public static Builder httpBuilder(@NotNull String vhost) {
        return new Builder(Proto.HTTP).setOptionInternal(VHOST, vhost);
    }

    @NotNull
    public Builder newTcpBuilder(int remotePort) {
        Builder builder = new Builder(Proto.TCP);
        builder.localAddr = localAddr;
        builder.localPort = localPort;
        builder.options = options;
        builder.setOptionInternal(REMOTE_PORT, String.valueOf(remotePort));
        return builder;
    }

    @NotNull
    public Builder newHttpBuilder(@NotNull String vhost) {
        Builder builder = new Builder(Proto.HTTP);
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
                return String.format("[tcp://%s:%d⇦%d?%s]", localAddr, localPort, remotePort(), map2String(options));
            case HTTP:
                return String.format("[http://%s:%d⇦%s?%s]", localAddr, localPort, vhost(), map2String(options));
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
        private Builder setOptionInternal(@NotNull String key, @NotNull String value) {
            if (options == null) {
                options = new HashMap<>();
            }
            options.put(key, value);
            return this;
        }

        @NotNull
        public Builder setOption(@NotNull String key, @NotNull String value) {
            if (key.startsWith("$")) {
                throw new IllegalArgumentException("$打头的key为系统保留的key");
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

    public interface Interceptor {
        Interceptor DEFAULT = new Interceptor() {
            @NotNull
            @Override
            public ProtoRequest proceed(@NotNull ProtoRequest request) throws ProtoException {
                return request;
            }
        };

        @NotNull
        ProtoRequest proceed(@NotNull ProtoRequest request) throws ProtoException;
    }

    public enum Proto {
        UNKNOWN((byte) 0x00),
        TCP((byte) 0x01),
        HTTP((byte) 0x02),
        ;

        private byte value;

        Proto(byte value) {
            this.value = value;
        }

        public byte getValue() {
            return value;
        }

        @NotNull
        public static Proto of(byte value) {
            final Proto[] values = values();
            for (Proto type : values) {
                if (type.value == value) {
                    return type;
                }
            }
            return Proto.UNKNOWN;
        }
    }

    @NotNull
    private static String map2String(Map<String, String> originalMap) {
        if (originalMap == null) {
            return "";
        }
        StringBuilder line = new StringBuilder();
        Set<Map.Entry<String, String>> entries = originalMap.entrySet();
        boolean first = true;
        for (Map.Entry<String, String> entry : entries) {
            String key = entry.getKey();
            String value = entry.getValue();
            if (key != null) {
                if (!first) {
                    line.append("&");
                }
                line.append(key);
                if (value != null) {
                    line.append("=");
                    line.append(value);
                }
            }
            first = false;
        }
        return line.toString();
    }

    @NotNull
    private static Map<String, String> string2Map(String originalLine) {
        if (originalLine == null) {
            return Collections.emptyMap();
        }
        String[] kvLines = originalLine.split("&");
        Map<String, String> map = new LinkedHashMap<>(kvLines.length);
        for (String it : kvLines) {
            String[] kvLine = it.split("=");
            if (kvLine.length == 1) {
                map.put(kvLine[0], null);
            } else if (kvLine.length == 2) {
                map.put(kvLine[0], kvLine[1]);
            }
        }
        return map;
    }

}
