package com.tuuzed.tunnel.interceptor;

import com.tuuzed.tunnel.proto.ProtoException;
import com.tuuzed.tunnel.proto.ProtoRequest;
import org.jetbrains.annotations.NotNull;

public interface ProtoRequestInterceptor {

    ProtoRequestInterceptor DEFAULT = new ProtoRequestInterceptor() {
        @NotNull
        @Override
        public ProtoRequest handleProtoRequest(@NotNull ProtoRequest request) throws ProtoException {
            return request;
        }
    };

    @NotNull
    ProtoRequest handleProtoRequest(@NotNull ProtoRequest request) throws ProtoException;

}
