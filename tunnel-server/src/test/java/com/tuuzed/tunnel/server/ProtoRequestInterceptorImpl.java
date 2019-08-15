package com.tuuzed.tunnel.server;

import com.tuuzed.tunnel.common.proto.ProtoException;
import com.tuuzed.tunnel.common.proto.ProtoRequest;
import com.tuuzed.tunnel.common.proto.ProtoRequestInterceptor;
import com.tuuzed.tunnel.common.util.PortUtils;
import org.jetbrains.annotations.NotNull;

public class ProtoRequestInterceptorImpl implements ProtoRequestInterceptor {
    @NotNull
    @Override
    public ProtoRequest proceed(@NotNull ProtoRequest request) throws ProtoException {
        if (request.isTcp()) {
            final String token = request.option("token");
            final int remotePort = request.remotePort();
            if (!"tk123456".equals(token)) {
                throw new ProtoException(String.format("request(%s), Bad Token(%s)", request.toString(), token));
            }
            if (!PortUtils.inPortRule("1024-60000", remotePort)) {
                throw new ProtoException(String.format("request(%s), remotePort(%s) Not allowed to use.", request.toString(), remotePort));
            }
            if (remotePort == 20000) {
                // 替换远程端口
                return request.cloneTcpBuilder(20080).build();
            }
            return request;
        }
        return request;
    }
}
