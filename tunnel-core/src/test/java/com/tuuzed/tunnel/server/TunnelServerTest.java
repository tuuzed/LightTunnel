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
            public boolean proceed(@NotNull OpenTunnelRequest request, @NotNull String[] errorMessage) {
                String token = request.arguments.get("token");
                if (!"tk123456".equals(token)) {
                    errorMessage[0] = "Token Error";
                    return false;
                }
                if (request.remotePort < 1024) {
                    errorMessage[0] = "remotePort Error";
                    return false;
                }
                return true;
            }
        }).start();
    }

}