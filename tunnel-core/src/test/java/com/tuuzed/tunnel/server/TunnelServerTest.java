package com.tuuzed.tunnel.server;

import com.tuuzed.tunnel.common.Interceptor;
import com.tuuzed.tunnel.common.protocol.OpenTunnelRequest;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

public class TunnelServerTest {
    @Test
    public void start() throws Exception {
        new TunnelServer(null, 4000, new Interceptor<OpenTunnelRequest>() {
            @Override
            public void proceed(@NotNull OpenTunnelRequest request) throws Exception {
                String token = request.options.get("token");
                if (!"tk123456".equals(token)) {
                    throw new Exception("Token Error");
                }
                if (request.remotePort < 1024) {
                    throw new Exception("remotePort Error");
                }
            }
        }).start();
    }

}