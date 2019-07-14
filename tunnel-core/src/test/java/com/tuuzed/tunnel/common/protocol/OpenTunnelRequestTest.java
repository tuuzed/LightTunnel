package com.tuuzed.tunnel.common.protocol;

import com.tuuzed.tunnel.common.TunnelProtocolException;
import org.junit.Test;

import static org.junit.Assert.*;

public class OpenTunnelRequestTest {

    @Test
    public void create() throws TunnelProtocolException {
        String uriString = "tcp://139.199.221.244:22?token=tk123456#10022";
        OpenTunnelRequest openTunnelRequest = OpenTunnelRequest.create(uriString);
        System.out.println(openTunnelRequest.localAddr);
        System.out.println(openTunnelRequest.localPort);
        System.out.println(openTunnelRequest.remotePort);
        System.out.println(openTunnelRequest.arguments);
        System.out.println(openTunnelRequest);
    }
}