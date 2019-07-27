package com.tuuzed.tunnel.common.protocol;

import io.netty.channel.Channel;
import io.netty.util.AttributeKey;

import java.util.concurrent.atomic.AtomicLong;

public final class TunnelAttributeKey {

    public static final AttributeKey<String> SUB_DOMAIN = AttributeKey.newInstance("sub_domain");


    public static final AttributeKey<Channel> NEXT_CHANNEL = AttributeKey.newInstance("next_channel");
    public static final AttributeKey<Long> TUNNEL_TOKEN = AttributeKey.newInstance("tunnel_token");

    public static final AttributeKey<AtomicLong> SESSION_TOKEN_PRODUCER = AttributeKey.newInstance("session_token_producer");

    public static final AttributeKey<Long> SESSION_TOKEN = AttributeKey.newInstance("session_token");
    public static final AttributeKey<OpenTunnelRequest> OPEN_TUNNEL_REQUEST = AttributeKey.newInstance("open_tunnel_request");
    public static final AttributeKey<Boolean> OPEN_TUNNEL_FAIL_FLAG = AttributeKey.newInstance("open_tunnel_fail_flag");
    public static final AttributeKey<String> OPEN_TUNNEL_FAIL_MESSAGE = AttributeKey.newInstance("open_tunnel_fail_message");


}
