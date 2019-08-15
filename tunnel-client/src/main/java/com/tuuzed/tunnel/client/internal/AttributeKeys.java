package com.tuuzed.tunnel.client.internal;

import com.tuuzed.tunnel.client.TunnelClientDescriptor;
import com.tuuzed.tunnel.common.proto.ProtoRequest;
import io.netty.channel.Channel;
import io.netty.util.AttributeKey;
import org.jetbrains.annotations.NotNull;

public final class AttributeKeys {

    @NotNull
    public static final AttributeKey<Long> SESSION_TOKEN = AttributeKey.newInstance("$SESSION_TOKEN");

    @NotNull
    public static final AttributeKey<Channel> NEXT_CHANNEL = AttributeKey.newInstance("$NEXT_CHANNEL");

    @NotNull
    public static final AttributeKey<Long> TUNNEL_TOKEN = AttributeKey.newInstance("$TUNNEL_TOKEN");

    @NotNull
    public static final AttributeKey<ProtoRequest> PROTO_REQUEST = AttributeKey.newInstance("$PROTO_REQUEST");

    @NotNull
    public static final AttributeKey<Boolean> FATAL_FLAG = AttributeKey.newInstance("$FATAL_FLAG");

    @NotNull
    public static final AttributeKey<Throwable> FATAL_CAUSE = AttributeKey.newInstance("$FATAL_CAUSE");


    public static final AttributeKey<TunnelClientDescriptor> TUNNEL_CLIENT_DESCRIPTOR = AttributeKey.newInstance("$TUNNEL_CLIENT_DESCRIPTOR");


}
