package com.tuuzed.tunnel.common.protocol;

import com.tuuzed.tunnel.common.TunnelProtocolException;
import org.junit.Test;

import java.nio.charset.StandardCharsets;

public class OpenTunnelRequestTest {

    @Test
    public void fromBytes() throws TunnelProtocolException {
        String uriString = "tcp://139.199.221.244:22?token=tk123456#10022";
        OpenTunnelRequest openTunnelRequest = OpenTunnelRequest.fromBytes(uriString.getBytes(StandardCharsets.UTF_8));
        System.out.println(openTunnelRequest.localAddr);
        System.out.println(openTunnelRequest.localPort);
        System.out.println(openTunnelRequest.remotePort);
        System.out.println(openTunnelRequest.arguments);
        System.out.println(openTunnelRequest);
    }
}