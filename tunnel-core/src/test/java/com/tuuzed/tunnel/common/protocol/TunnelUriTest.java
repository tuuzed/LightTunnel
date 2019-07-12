package com.tuuzed.tunnel.common.protocol;

import org.junit.Test;

import static org.junit.Assert.*;

public class TunnelUriTest {

    @Test
    public void create() {
        String uriString = "tcp://127.0.0.1:8080?remotePort=18080&&&&token=123&token=中国&&";
        TunnelUri tunnelUri = TunnelUri.create(uriString);
        System.out.println(tunnelUri.queryMap);
        System.out.println(tunnelUri);
    }
}