package com.tuuzed.tunnel.common.protocol;

import org.junit.Test;

public class TunnelUriTest {

    @Test
    public void create() {
        String uriString = "tcp://139.199.221.244:22?token=tk123456#10022";
        OpenTunnelRequest tunnelUri = OpenTunnelRequest.create(uriString);
        System.out.println(tunnelUri.localAddr);
        System.out.println(tunnelUri.localPort);
        System.out.println(tunnelUri.remotePort);
        System.out.println(tunnelUri.options);
        System.out.println(tunnelUri);
    }
}