package com.tuuzed.tunnel.server;

import com.tuuzed.tunnel.common.Interceptor;
import com.tuuzed.tunnel.common.TunnelProtocolException;
import com.tuuzed.tunnel.common.protocol.OpenTunnelRequest;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

public class TunnelServerTest {
    @Test
    public void start() throws Exception {
        new TunnelServer(null, 4000, new Interceptor<OpenTunnelRequest>() {
            @NotNull
            @Override
            public OpenTunnelRequest proceed(@NotNull OpenTunnelRequest request) throws TunnelProtocolException {
                String token = request.arguments.get("token");
                if (!"tk123456".equals(token)) {
                    throw new TunnelProtocolException("Token Error");
                }
                if (request.remotePort < 1024) {
                    throw new TunnelProtocolException("remotePort Error");
                }
                if (request.remotePort == 10080) {
                    // 替换远程端口
                    return new OpenTunnelRequest(
                            request.type,
                            request.localAddr, request.localPort,
                            20080,
                            request.arguments
                    );
                }
                return request;
            }
        }).start();
    }

}