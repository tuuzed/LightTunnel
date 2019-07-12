package com.tuuzed.tunnel.common.protocol;

import org.junit.Test;

public class TunnelUriTest {

    @Test
    public void create() {
        String uriString = "tcp://127.0.0.1:8080?token=123&token=中国&a=#18080";
        OpenTunnelRequest tunnelUri = OpenTunnelRequest.create(uriString);
        System.out.println(tunnelUri.localAddr);
        System.out.println(tunnelUri.localPort);
        System.out.println(tunnelUri.remotePort);
        System.out.println(tunnelUri.options);
        System.out.println(tunnelUri);
    }
}