package com.tuuzed.tunnel.server;

import com.tuuzed.tunnel.common.protocol.OpenTunnelRequest;
import com.tuuzed.tunnel.common.protocol.OpenTunnelRequestInterceptor;
import com.tuuzed.tunnel.common.protocol.TunnelProtocolException;
import com.tuuzed.tunnel.common.util.PortUtils;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

public class TunnelServerTest {
    @Test
    public void start() throws Exception {
        new TunnelServer(null, 4000, new OpenTunnelRequestInterceptor() {
            @NotNull
            @Override
            public OpenTunnelRequest proceed(@NotNull OpenTunnelRequest request) throws TunnelProtocolException {
                String token = request.arguments.get("token");
                if (!"tk123456".equals(token)) {
                    throw new TunnelProtocolException(String.format("request(%s), Bad Token(%s)",request.toString(), token));
                }
                if (!PortUtils.inPortRule("1024-60000", request.remotePort)) {
                    throw new TunnelProtocolException(String.format("request(%s), remotePort(%s) Not allowed to use.", request.toString(),request.remotePort));
                }
                if (request.remotePort == 20000) {
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